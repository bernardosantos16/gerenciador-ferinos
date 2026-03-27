package com.bernardo.geradortimes.user.service;

import com.bernardo.geradortimes.shared.value_object.Email;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.shared.value_object.PasswordHash;
import com.bernardo.geradortimes.shared.api.FieldValidationException;
import com.bernardo.geradortimes.shared.security.PasswordService;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.response.UserResponseDTO;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    public UserResponseDTO create(CreateUserRequestDTO request) {
        String nicknameRaw = request.nickname() == null ? null : request.nickname().trim();
        String loginRaw = request.login() == null ? null : request.login().trim();

        if (userRepository.existsByNickname_Value(nicknameRaw)) {
            log.error("Erro ao criar no usuário - nickname {} já é utilizado", nicknameRaw);
            throw new FieldValidationException(CONFLICT, "nickname", "nickname already exists");
        }
        if (userRepository.existsByLogin_Value(loginRaw)) {
            log.error("Erro ao criar no usuário - login {} já é utilizado", loginRaw);
            throw new FieldValidationException(CONFLICT, "login", "login already exists");
        }

        Nickname nickname = Nickname.of(nicknameRaw);
        Email email = Email.of(loginRaw);
        PasswordHash passwordHash = PasswordHash.fromEncoded(passwordService.hash(request.password()));

        User user = User.create(request.name(), nickname, email, passwordHash);
        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> list() {
        return userRepository.findAll().stream().map(UserService::toResponse).toList();
    }

    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "user not found");
        }
        userRepository.deleteById(id);
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
