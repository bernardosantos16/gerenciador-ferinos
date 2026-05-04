package com.bernardo.geradortimes.user.service;

import com.bernardo.geradortimes.shared.value_object.Email;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.shared.value_object.PasswordHash;
import com.bernardo.geradortimes.shared.api.FieldValidationException;
import com.bernardo.geradortimes.shared.security.PasswordService;
import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.response.UserResponseDTO;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.rabbitmq.UserRegisteredEvent;
import com.bernardo.geradortimes.user.rabbitmq.UserRegisteredProducer;
import com.bernardo.geradortimes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final CurrentUserService currentUserService;
    private final UserRegisteredProducer userRegisteredProducer;

    public UserService(UserRepository userRepository, PasswordService passwordService, CurrentUserService currentUserService, UserRegisteredProducer userRegisteredProducer) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.currentUserService = currentUserService;
        this.userRegisteredProducer = userRegisteredProducer;
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
        String verificationToken = user.generateEmailVerificationToken();
        User saved = userRepository.save(user);

        userRegisteredProducer.publish(new UserRegisteredEvent(saved.getId(), saved.getName(), email.getValue(), verificationToken));

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

    @Transactional
    public void verifyEmailToken(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "invalid or expired verification token"));

        boolean verified = user.verifyEmail(token);
        if (!verified) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid verification token");
        }
        userRepository.save(user);
        log.info("Email verificado com sucesso - userId: {}", user.getId());
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
