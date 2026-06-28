package com.bernardo.geradortimes.user.service;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.shared.value_object.Email;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.shared.value_object.PasswordHash;
import com.bernardo.geradortimes.shared.api.FieldValidationException;
import com.bernardo.geradortimes.shared.security.PasswordService;
import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.response.UserResponseDTO;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.rabbitmq.PasswordResetEvent;
import com.bernardo.geradortimes.user.rabbitmq.PasswordResetProducer;
import com.bernardo.geradortimes.user.rabbitmq.UserRegisteredEvent;
import com.bernardo.geradortimes.user.rabbitmq.UserRegisteredProducer;
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

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final VerificationTokenService verificationTokenService;
    private final PasswordService passwordService;
    private final CurrentUserService currentUserService;
    private final UserRegisteredProducer userRegisteredProducer;
    private final PasswordResetProducer passwordResetProducer;

    public UserService(UserRepository userRepository, VerificationTokenRepository verificationTokenRepository, VerificationTokenService verificationTokenService, PasswordService passwordService, CurrentUserService currentUserService, UserRegisteredProducer userRegisteredProducer, PasswordResetProducer passwordResetProducer) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.verificationTokenService = verificationTokenService;
        this.passwordService = passwordService;
        this.currentUserService = currentUserService;
        this.userRegisteredProducer = userRegisteredProducer;
        this.passwordResetProducer = passwordResetProducer;
    }

    public UserResponseDTO create(CreateUserRequestDTO request) {
        String nicknameRaw = request.nickname() == null ? null : request.nickname().trim();
        String loginRaw = request.login() == null ? null : request.login().trim();

        if (userRepository.existsByNickname_Value(nicknameRaw)) {
            log.warn("Erro ao criar usuario - nickname ja utilizado");
            throw new FieldValidationException(CONFLICT, "nickname", "nickname already exists");
        }
        if (userRepository.existsByLogin_Value(loginRaw)) {
            log.warn("Erro ao criar usuario - login ja utilizado");
            throw new FieldValidationException(CONFLICT, "login", "login already exists");
        }

        Nickname nickname = Nickname.of(nicknameRaw);
        Email email = Email.of(loginRaw);
        PasswordHash passwordHash = PasswordHash.fromEncoded(passwordService.hash(request.password()));

        User user = User.create(request.name(), nickname, email, passwordHash);
        User saved = userRepository.save(user);

        String token = verificationTokenService.issueAccountVerificationToken(saved.getId());

        userRegisteredProducer.publish(new UserRegisteredEvent(saved.getId(), nickname.getValue(), email.getValue(), token));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(UUID id) {
        UUID currentId = currentUserService.requireUserId();
        if (!currentId.equals(id) && !currentUserService.isAdmin()) {
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
            throw new ResponseStatusException(FORBIDDEN, "forbidden");
        }
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "user not found");
        }
        userRepository.deleteById(id);
    }

    public void verifyEmailToken(String token) {
        UUID userId = verificationTokenService.verifyAccountToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        user.activateUser();
        userRepository.save(user);

        log.info("Email verificado com sucesso - userId: {}", user.getId());
    }

    public void forgotPassword(String login) {
        String loginTrimmed = login == null ? null : login.trim();

        User user = userRepository.findByLogin_Value(loginTrimmed).orElse(null);
        if (user == null) {
            log.info("Tentativa de recuperacao de senha para email nao encontrado");
            return;
        }

        if (verificationTokenRepository.findActiveByUserIdAndType(
                user.getId(), TokenType.PASSWORD_RESET, Instant.now()).isPresent()) {
            log.info("Token de recuperacao de senha ainda ativo - ignorando nova solicitacao - userId: {}", user.getId());
            return;
        }

        String token = verificationTokenService.issuePasswordResetToken(user.getId());

        passwordResetProducer.publish(new PasswordResetEvent(
                user.getId(),
                user.getLogin().getValue(),
                token
        ));

        log.info("Token de recuperacao de senha gerado - userId: {}", user.getId());
    }

    public void resendVerification(String login) {
        String loginTrimmed = login == null ? null : login.trim();

        User user = userRepository.findByLogin_Value(loginTrimmed).orElse(null);
        if (user == null || user.getStatus() != ActivityStatus.PENDING) {
            log.info("Tentativa de reenvio de verificacao ignorada - email nao encontrado ou ja ativo");
            return;
        }

        if (verificationTokenRepository.findActiveByUserIdAndType(
                user.getId(), TokenType.ACCOUNT_VERIFICATION, Instant.now()).isPresent()) {
            log.info("Token de verificacao ainda ativo - ignorando nova solicitacao - userId: {}", user.getId());
            return;
        }

        String token = verificationTokenService.issueAccountVerificationToken(user.getId());

        userRegisteredProducer.publish(new UserRegisteredEvent(
                user.getId(),
                user.getNickname().getValue(),
                user.getLogin().getValue(),
                token
        ));

        log.info("Token de verificacao reenviado - userId: {}", user.getId());
    }

    public void resetPassword(String token, String newPassword) {
        UUID userId = verificationTokenService.verifyPasswordResetToken(token);

        User user = userRepository.findById(userId)
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
