package com.bernardo.geradortimes.auth.service;

import com.bernardo.geradortimes.auth.config.JwtProperties;
import com.bernardo.geradortimes.auth.model.RefreshToken;
import com.bernardo.geradortimes.auth.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@Transactional
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final String refreshTokenSalt;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.refreshTokenSalt = jwtProperties.refreshTokenSalt();
        if (refreshTokenSalt == null || refreshTokenSalt.isBlank()) {
            throw new IllegalStateException("auth.jwt.refresh-token-salt must be configured");
        }
        if (refreshTokenSalt.length() < 32) {
            throw new IllegalStateException("auth.jwt.refresh-token-salt must be at least 32 characters");
        }
    }

    public String issue(UUID userId) {
        String token = generateToken();
        String tokenHash = hashToken(token);
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(RefreshToken.issue(userId, tokenHash, expiresAt));
        log.info("Refresh token emitido - userId: {}", userId);
        return token;
    }

    public String rotate(RefreshToken current) {
        current.revoke();
        refreshTokenRepository.save(current);
        log.info("Refresh token rotacionado - userId: {}", current.getUserId());
        return issue(current.getUserId());
    }

    public Optional<RefreshToken> findByTokenValue(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hashToken(refreshTokenValue);
        return refreshTokenRepository.findByTokenForUpdate(tokenHash);
    }

    public void revokeByTokenValue(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }
        String tokenHash = hashToken(refreshTokenValue);
        refreshTokenRepository.findByTokenForUpdate(tokenHash).ifPresentOrElse(
                rt -> {
                    rt.revoke();
                    refreshTokenRepository.save(rt);
                    log.info("Refresh token revogado - userId: {}", rt.getUserId());
                },
                () -> log.warn("Revogacao de refresh token ignorada - token nao encontrado")
        );
    }

    private static String generateToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    refreshTokenSalt.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] digest = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("unable to hash refresh token", e);
        }
    }

    @Scheduled(fixedRate = 3600_000)
    public void cleanRevokedOrExpired() {
        int deleted = refreshTokenRepository.deleteRevokedOrExpired();
        if (deleted > 0) {
            log.info("Limpeza de refresh tokens revogados/expirados - removidos: {}", deleted);
        } else {
            log.debug("Limpeza de refresh tokens revogados/expirados executada - nenhum removido");
        }
    }
}
