package com.bernardo.geradortimes.user.service;

import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.user.model.VerificationToken;
import com.bernardo.geradortimes.user.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@Transactional
public class VerificationTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

//    @Value("${app.verification.token.expiration-minutes}")
    private static final int EXPIRATION_MINUTES = 15;

    private final VerificationTokenRepository verificationTokenRepository;

    public VerificationTokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    /**
     * Gera um token numerico de 6 digitos, aplica hash SHA-256, persiste um
     * {@link VerificationToken} do tipo {@link TokenType#ACCOUNT_VERIFICATION}
     * com expiracao em minutos e retorna o token em texto puro.
     */
    public String issueAccountVerificationToken(UUID userId) {
        return issueToken(userId, TokenType.ACCOUNT_VERIFICATION);
    }

    /**
     * Gera um token numerico de 6 digitos, aplica hash SHA-256, persiste um
     * {@link VerificationToken} do tipo {@link TokenType#PASSWORD_RESET}
     * com expiracao de {@value EXPIRATION_MINUTES} minutos e retorna o token em texto puro.
     */
    public String issuePasswordResetToken(UUID userId) {
        return issueToken(userId, TokenType.PASSWORD_RESET);
    }

    /**
     * Valida um token de verificacao de conta: faz hash SHA-256 do token recebido,
     * busca no banco, verifica expiracao e uso, marca como usado e retorna o userId.
     */
    public UUID verifyAccountToken(String token) {
        return verifyToken(token, TokenType.ACCOUNT_VERIFICATION);
    }

    /**
     * Valida um token de recuperacao de senha: faz hash SHA-256 do token recebido,
     * busca no banco, verifica expiracao e uso, marca como usado e retorna o userId.
     */
    public UUID verifyPasswordResetToken(String token) {
        return verifyToken(token, TokenType.PASSWORD_RESET);
    }

    private String issueToken(UUID userId, TokenType type) {
        String token = generateNumericToken();
        String tokenHash = sha256(token);
        Instant expiresAt = Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        VerificationToken vt = VerificationToken.create(tokenHash, type, expiresAt, userId);
        verificationTokenRepository.save(vt);

        log.info("Token gerado - type: {} userId: {}", type, userId);
        return token;
    }

    private UUID verifyToken(String token, TokenType type) {
        String tokenHash = sha256(token);

        VerificationToken vt = verificationTokenRepository
                .findByTokenHashAndType(tokenHash, type)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "invalid or expired verification token"));

        if (vt.isExpired()) {
            throw new ResponseStatusException(UNAUTHORIZED, "verification token expired");
        }
        if (vt.isUsed()) {
            throw new ResponseStatusException(UNAUTHORIZED, "verification token already used");
        }

        vt.markUsed();
        verificationTokenRepository.save(vt);

        log.info("Token consumido - type: {} userId: {}", type, vt.getUserId());
        return vt.getUserId();
    }

    private static String generateNumericToken() {
        int token = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(token);
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
