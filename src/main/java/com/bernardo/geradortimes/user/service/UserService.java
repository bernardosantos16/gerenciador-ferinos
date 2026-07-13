package com.bernardo.geradortimes.user.service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.auth.security.JwtService;
import com.bernardo.geradortimes.shared.api.FieldValidationException;
import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.shared.observability.LogSanitizer;
import com.bernardo.geradortimes.shared.security.PasswordService;
import com.bernardo.geradortimes.shared.value_object.Email;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.shared.value_object.PasswordHash;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.response.UserResponseDTO;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.rabbitmq.email_verification.EmailVerificationEvent;
import com.bernardo.geradortimes.user.rabbitmq.email_verification.EmailVerificationProducer;
import com.bernardo.geradortimes.user.rabbitmq.password_reset.PasswordResetEvent;
import com.bernardo.geradortimes.user.rabbitmq.password_reset.PasswordResetProducer;
import com.bernardo.geradortimes.user.repository.UserRepository;
import com.bernardo.geradortimes.user.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final VerificationTokenService verificationTokenService;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final CurrentUserService currentUserService;
    private final PasswordResetProducer passwordResetProducer;
    private final EmailVerificationProducer emailVerificationProducer;

    public UserService(UserRepository userRepository, VerificationTokenRepository verificationTokenRepository, VerificationTokenService verificationTokenService, JwtService jwtService, PasswordService passwordService, CurrentUserService currentUserService, PasswordResetProducer passwordResetProducer, EmailVerificationProducer emailVerificationProducer) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.verificationTokenService = verificationTokenService;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
        this.currentUserService = currentUserService;
        this.passwordResetProducer = passwordResetProducer;
        this.emailVerificationProducer = emailVerificationProducer;
    }

    @Transactional
    public void sendEmailVerification(String login) {
        String loginTrimmed = login == null ? null : login.trim();

        if (userRepository.existsByLogin_Value(loginTrimmed)) {
            log.warn("Verificacao de email rejeitada - email ja cadastrado - email: {}", LogSanitizer.maskEmail(loginTrimmed));
            throw new FieldValidationException(CONFLICT, "login", "email already registered");
        }

        String token = verificationTokenService.issueEmailVerificationToken(loginTrimmed);

        emailVerificationProducer.publish(new EmailVerificationEvent(loginTrimmed, token));

        log.info("Token de verificacao de email publicado para envio - email: {}", LogSanitizer.maskEmail(loginTrimmed));
    }

    public String verifyEmail(String login, String token) {
        String loginTrimmed = login == null ? null : login.trim();
        verificationTokenService.verifyEmailToken(token, loginTrimmed, true);
        log.info("Email verificado com sucesso, emitindo token de registro - email: {}", LogSanitizer.maskEmail(loginTrimmed));
        return jwtService.issueRegistrationToken(loginTrimmed);
    }

    public UserResponseDTO create(CreateUserRequestDTO request) {
        String nicknameRaw = request.nickname() == null ? null : request.nickname().trim();
        String loginRaw;
        try {
            loginRaw = jwtService.verifyRegistrationToken(request.registrationToken());
        } catch (JWTVerificationException e) {
            log.warn("Token de registro invalido ou expirado");
            throw new ResponseStatusException(NOT_FOUND, "invalid or expired registration token");
        }

        if (userRepository.existsByNickname_Value(nicknameRaw)) {
            log.warn("Criacao de usuario rejeitada - conflictField: nickname, conflictReason: ALREADY_EXISTS");
            throw new FieldValidationException(CONFLICT, "nickname", "nickname already exists");
        }
        if (userRepository.existsByLogin_Value(loginRaw)) {
            log.warn("Criacao de usuario rejeitada - conflictField: login, conflictReason: ALREADY_EXISTS");
            throw new FieldValidationException(CONFLICT, "login", "login already exists");
        }

        Nickname nickname = Nickname.of(nicknameRaw);
        Email email = Email.of(loginRaw);
        PasswordHash passwordHash = PasswordHash.fromEncoded(passwordService.hash(request.password()));

        User user = User.create(request.name(), nickname, email, passwordHash);
        user.activateUser();
        User saved = userRepository.save(user);
        log.info("Usuario criado e ativado - userId: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(UUID id) {
        UUID currentId = currentUserService.requireUserId();
        if (!currentId.equals(id) && !currentUserService.isAdmin()) {
            log.warn("Acesso negado a dados de usuario - currentUserId: {}, targetUserId: {}", currentId, id);
            throw new ResponseStatusException(FORBIDDEN, "forbidden");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> list(Pageable pageable) {
        if (currentUserService.isAdmin()) {
            return userRepository.findAll(pageable).map(UserService::toResponse);
        }
        UUID currentId = currentUserService.requireUserId();
        User user = userRepository.findById(currentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        List<UserResponseDTO> content = List.of(toResponse(user));
        long total = 1;
        if (pageable.getOffset() >= total) {
            return Page.empty(pageable);
        }
        return new PageImpl<>(content, pageable, total);
    }

    public void delete(UUID id) {
        UUID currentId = currentUserService.requireUserId();
        if (!currentId.equals(id) && !currentUserService.isAdmin()) {
            log.warn("Delecao de usuario negada - currentUserId: {}, targetUserId: {}", currentId, id);
            throw new ResponseStatusException(FORBIDDEN, "forbidden");
        }
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "user not found");
        }
        userRepository.deleteById(id);
        log.info("Usuario deletado - userId: {}, por currentUserId: {}", id, currentId);
    }

    public void forgotPassword(String login) {
        String loginTrimmed = login == null ? null : login.trim();

        User user = userRepository.findByLogin_Value(loginTrimmed).orElse(null);
        if (user == null) {
            log.info("Recuperacao de senha solicitada para email nao cadastrado - email: {}", LogSanitizer.maskEmail(loginTrimmed));
            return;
        }

        if (verificationTokenRepository.findActiveByEmailAndType(
                loginTrimmed, TokenType.PASSWORD_RESET, Instant.now()).isPresent()) {
            log.info("Token de recuperacao de senha ainda ativo - ignorando nova solicitacao - userId: {}", user.getId());
            return;
        }

        String token = verificationTokenService.issuePasswordResetToken(loginTrimmed);

        passwordResetProducer.publish(new PasswordResetEvent(
                user.getId(),
                loginTrimmed,
                token
        ));

        log.info("Token de recuperacao de senha gerado - userId: {}", user.getId());
    }

    public void resetPassword(String email, String token, String newPassword) {
        String emailTrimmed = email == null ? null : email.trim();
        verificationTokenService.verifyPasswordResetToken(token, emailTrimmed);

        User user = userRepository.findByLogin_Value(emailTrimmed)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        String hash = passwordService.hash(newPassword);
        user.updatePassword(PasswordHash.fromEncoded(hash));
        userRepository.save(user);

        log.info("Senha redefinida com sucesso - userId: {}", user.getId());
    }

    private static UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getNickname().getValue(),
                user.getLogin().getValue()
        );
    }

}
