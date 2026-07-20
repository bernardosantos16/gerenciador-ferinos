package com.bernardo.geradortimes.user.service;

import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.shared.observability.LogSanitizer;
import com.bernardo.geradortimes.user.model.VerificationToken;
import com.bernardo.geradortimes.user.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${argon.hash.pepper}")
    private String tokenPepper;

    public VerificationTokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    /**
     * Gera um token numerico de 6 digitos, gera um salt aleatorio de 256 bits,
     * aplica hash SHA-256(token + pepper + salt), persiste um
     * {@link VerificationToken} do tipo {@link TokenType#EMAIL_VERIFICATION}
     * com expiracao de {@value EXPIRATION_MINUTES} minutos e retorna o token em texto puro.
     */
    public String issueEmailVerificationToken(String email) {
        String token = generateNumericToken();
        String salt = generateSalt();
        String tokenHash = hashToken(token, salt);
        Instant expiresAt = Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        verificationTokenRepository.
                findActiveByEmailAndType(email, TokenType.EMAIL_VERIFICATION, Instant.now()).ifPresentOrElse(
                        verificationToken -> {
                            verificationToken.setTokenHash(tokenHash);
                            verificationToken.setTokenSalt(salt);
                            verificationToken.setExpiresAt(expiresAt);
                            log.info("Token de verificacao de email atualizado - email: {}", LogSanitizer.maskEmail(email));
                        },
                        () -> {
                            VerificationToken verificationToken = VerificationToken.create(
                                    tokenHash,
                                    TokenType.EMAIL_VERIFICATION,
                                    expiresAt,
                                    email,
                                    salt
                            );
                            verificationTokenRepository.save(verificationToken);
                            log.info("Token de verificacao de email gerado - email: {}", LogSanitizer.maskEmail(email));
                        }
                );
        return token;
    }

    /**
     * Valida um token de verificacao de email.
     * Busca o token ativo por email + tipo, recupera o salt, computa o hash
     * e compara. Lanca excecao se o token for invalido, expirado ou ja usado.
     */
    public void verifyEmailToken(String token, String email, Boolean consume) {
        VerificationToken vt = findAndVerifyToken(token, email, TokenType.EMAIL_VERIFICATION);

        if (consume) {
            vt.markUsed();
            verificationTokenRepository.save(vt);
        }
    }

    /**
     * Gera um token numerico de 6 digitos, gera um salt aleatorio, aplica hash
     * SHA-256(token + pepper + salt), persiste um {@link VerificationToken} do tipo
     * {@link TokenType#PASSWORD_RESET} com expiracao de {@value EXPIRATION_MINUTES}
     * minutos e retorna o token em texto puro.
     */
    public String issuePasswordResetToken(String email) {
        String token = generateNumericToken();
        String salt = generateSalt();
        String tokenHash = hashToken(token, salt);
        Instant expiresAt = Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        VerificationToken vt = VerificationToken.create(tokenHash, TokenType.PASSWORD_RESET, expiresAt, email, salt);
        verificationTokenRepository.save(vt);

        log.info("Token de recuperacao de senha gerado - email: {}", LogSanitizer.maskEmail(email));
        return token;
    }

    /**
     * Valida um token de recuperacao de senha: busca o token ativo por email + tipo,
     * recupera o salt, computa o hash e compara com o armazenado. Em caso de sucesso,
     * marca como usado.
     */
    public void verifyPasswordResetToken(String token, String email) {
        VerificationToken vt = findAndVerifyToken(token, email, TokenType.PASSWORD_RESET);

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

    /**
     * Busca o token ativo (nao expirado, nao usado) por email + tipo com lock
     * pessimista, recupera o salt, computa o hash do token submetido e compara.
     * Unifica a mensagem de erro para todos os casos de falha (nao distingue
     * "token nao encontrado" de "salt/hash mismatch").
     */
    private VerificationToken findAndVerifyToken(String rawToken, String email, TokenType type) {
        VerificationToken vt = verificationTokenRepository
                .findActiveByEmailAndTypeForUpdate(email, type, Instant.now())
                .orElseThrow(() -> {
                    log.warn("Token invalido ou expirado - type: {}, email: {}", type, LogSanitizer.maskEmail(email));
                    return new ResponseStatusException(NOT_FOUND, "invalid or expired verification token");
                });

        String expectedHash = hashToken(rawToken, vt.getTokenSalt());
        if (!expectedHash.equals(vt.getTokenHash())) {
            log.warn("Token invalido - hash mismatch - type: {}, email: {}", type, LogSanitizer.maskEmail(email));
            throw new ResponseStatusException(NOT_FOUND, "invalid or expired verification token");
        }

        return vt;
    }

    private String hashToken(String token, String salt) {
        return sha256(token + tokenPepper + salt);
    }

    private static String generateNumericToken() {
        int token = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(token);
    }

    private static String generateSalt() {
        byte[] bytes = new byte[32]; // 256 bits
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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
