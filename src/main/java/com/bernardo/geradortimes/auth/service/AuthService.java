package com.bernardo.geradortimes.auth.service;

import com.bernardo.geradortimes.auth.config.JwtProperties;
import com.bernardo.geradortimes.auth.dto.request.LoginRequestDTO;
import com.bernardo.geradortimes.auth.dto.request.RefreshTokenRequestDTO;
import com.bernardo.geradortimes.auth.model.RefreshToken;
import com.bernardo.geradortimes.auth.security.JwtService;
import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.security.PasswordService;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    public AuthTokens login(LoginRequestDTO request) {
        User user = userRepository.findByLogin_Value(request.login().trim())
                .orElseThrow(() -> {
                    log.error("Tentativa de login com login inexistente - login: {}", request.login());
                    return new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
                });



        boolean ok = passwordService.matches(request.password(), user.getPassword().getValue());
        if (!ok) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
        }

        ActivityStatus status = user.getStatus();
        if (status.isInvalid()) {
            log.error("Usuário com status {} tentou autenticar - userId: {}", status, user.getId());
            throw new ResponseStatusException(UNAUTHORIZED, status.getErrorMessage());
        }


        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());

        return new AuthTokens(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenTtl().toSeconds()
        );
    }

    public AuthTokens refresh(RefreshTokenRequestDTO request) {
        RefreshToken current = refreshTokenService.findByTokenValue(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid refresh token - token not found"));

        if (current.isRevoked() || current.isExpired()) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid refresh token - token revoked or expired");
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid refresh token - user not found"));

        if (user.getStatus() != ActivityStatus.ACTIVE) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid refresh token - user not active");
        }

        String accessToken = jwtService.issueAccessToken(user);
        String newRefresh = refreshTokenService.rotate(current);

        return new AuthTokens(
                accessToken,
                newRefresh,
                jwtProperties.accessTokenTtl().toSeconds()
        );
    }
}
