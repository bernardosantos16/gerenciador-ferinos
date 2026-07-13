package com.bernardo.geradortimes.user.service;

import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.shared.observability.LogSanitizer;
import com.bernardo.geradortimes.user.model.VerificationToken;
import com.bernardo.geradortimes.user.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@Transactional
public class VerificationTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int EXPIRATION_MINUTES = 5;

    private final VerificationTokenRepository verificationTokenRepository;

    public VerificationTokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    /**
     * Gera um token numerico de 6 digitos, aplica hash SHA-256, persiste um
     * {@link VerificationToken} do tipo {@link TokenType#EMAIL_VERIFICATION}
     * com expiracao de {@value EXPIRATION_MINUTES} minutos e retorna o token em texto puro.
     */
    public String issueEmailVerificationToken(String email) {
        String token = generateNumericToken();
        String tokenHash = sha256(token);
        Instant expiresAt = Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        verificationTokenRepository.
                findActiveByEmailAndType(email, TokenType.EMAIL_VERIFICATION, Instant.now()).ifPresentOrElse(
                        verificationToken -> {
                            verificationToken.setTokenHash(tokenHash);
                            verificationToken.setExpiresAt(expiresAt);
                            log.info("Token de verificacao de email atualizado - email: {}", LogSanitizer.maskEmail(email));
                        },
                        () -> {
                            VerificationToken verificationToken = VerificationToken.create(
                                    tokenHash,
                                    TokenType.EMAIL_VERIFICATION,
                                    expiresAt,
                                    email
                            );
                            verificationTokenRepository.save(verificationToken);
                            log.info("Token de verificacao de email gerado - email: {}", LogSanitizer.maskEmail(email));
                        }
                );
        return token;
    }

    /**
     * Valida um token de verificacao de email sem consumi-lo.
     * Lanca excecao se o token for invalido, expirado ou ja usado.
     */
    public void verifyEmailToken(String token, String email, Boolean consume) {
        String tokenHash = sha256(token);

        VerificationToken vt = verificationTokenRepository
                .findByTokenHashAndTypeAndEmail(tokenHash, TokenType.EMAIL_VERIFICATION, email)
                .orElseThrow(() -> {
                    log.warn("Token de verificacao de email invalido - email: {}", LogSanitizer.maskEmail(email));
                    return new ResponseStatusException(NOT_FOUND, "invalid or expired verification token");
                });

        if (vt.isExpired()) {
            log.warn("Token de verificacao de email expirado - email: {}", LogSanitizer.maskEmail(email));
            throw new ResponseStatusException(UNAUTHORIZED, "verification token expired");
        }
        if (vt.isUsed()) {
            log.warn("Token de verificacao de email ja utilizado - email: {}", LogSanitizer.maskEmail(email));
            throw new ResponseStatusException(UNAUTHORIZED, "verification token already used");
        }
        if (consume) {
            vt.markUsed();
            verificationTokenRepository.save(vt);
        }
    }

    /**
     * Gera um token numerico de 6 digitos, aplica hash SHA-256, persiste um
     * {@link VerificationToken} do tipo {@link TokenType#PASSWORD_RESET}
     * com expiracao de {@value EXPIRATION_MINUTES} minutos e retorna o token em texto puro.
     */
    public String issuePasswordResetToken(String email) {
        String token = generateNumericToken();
        String tokenHash = sha256(token);
        Instant expiresAt = Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        VerificationToken vt = VerificationToken.create(tokenHash, TokenType.PASSWORD_RESET, expiresAt, email);
        verificationTokenRepository.save(vt);

        log.info("Token de recuperacao de senha gerado - email: {}", LogSanitizer.maskEmail(email));
        return token;
    }

    /**
     * Valida um token de recuperacao de senha: faz hash SHA-256 do token recebido,
     * busca no banco, verifica expiracao e uso e marca como usado.
     */
    public void verifyPasswordResetToken(String token, String email) {
        String tokenHash = sha256(token);

        VerificationToken vt = verificationTokenRepository
                .findByTokenHashAndTypeAndEmail(tokenHash, TokenType.PASSWORD_RESET, email)
                .orElseThrow(() -> {
                    log.warn("Token de recuperacao de senha invalido - email: {}", LogSanitizer.maskEmail(email));
                    return new ResponseStatusException(NOT_FOUND, "invalid or expired verification token");
                });

        if (vt.isExpired()) {
            log.warn("Token de recuperacao de senha expirado - email: {}", LogSanitizer.maskEmail(email));
            throw new ResponseStatusException(UNAUTHORIZED, "verification token expired");
        }
        if (vt.isUsed()) {
            log.warn("Token de recuperacao de senha ja utilizado - email: {}", LogSanitizer.maskEmail(email));
            throw new ResponseStatusException(UNAUTHORIZED, "verification token already used");
        }

        vt.markUsed();
        verificationTokenRepository.save(vt);

        log.info("Token de recuperacao de senha consumido - email: {}", LogSanitizer.maskEmail(email));
    }

    /**
     * Remove tokens expirados ou ja consumidos a cada hora.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cleanExpiredOrUsedTokens() {
        verificationTokenRepository.deleteByExpiresAtBeforeOrUsedAtIsNotNull(Instant.now());
        log.debug("Limpeza de tokens expirados/consumidos executada");
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
