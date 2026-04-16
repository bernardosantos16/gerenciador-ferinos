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
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid credentials"));

        if (user.getStatus() == ActivityStatus.INACTIVE) {
            log.error("Usuario inativo tentou autenticar - userId: {}", user.getId());
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
        }

        boolean ok = passwordService.matches(request.password(), user.getPassword().getValue());
        if (!ok) {
            log.error("Senha incorreta para usuario - userId: {}", user.getId());
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
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
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid refresh token"));

        if (current.isRevoked() || current.isExpired()) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid refresh token");
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid refresh token"));

        if (user.getStatus() == ActivityStatus.INACTIVE) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid refresh token");
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
