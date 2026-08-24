package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.club.dto.response.InviteTokenResponseDTO;
import com.bernardo.geradortimes.club.model.ClubInviteToken;
import com.bernardo.geradortimes.club.repository.ClubInviteTokenRepository;
import com.bernardo.geradortimes.shared.security.AesGcmCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Service
@Transactional
public class ClubInviteTokenService {

    private static final String TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ClubInviteTokenRepository clubInviteTokenRepository;
    private final AesGcmCipher cipher;

    @Value("${app.invite-token.ttl-minutes:10080}")
    private int ttlMinutes;

    public ClubInviteTokenService(
            ClubInviteTokenRepository clubInviteTokenRepository,
            @Value("${app.invite-token.encryption-key}") String encryptionKey
    ) {
        this.clubInviteTokenRepository = clubInviteTokenRepository;
        this.cipher = AesGcmCipher.fromBase64(encryptionKey);
    }

    public InviteTokenResponseDTO getCurrent(UUID clubId) {
        return clubInviteTokenRepository.findByClubId(clubId)
                .filter(token -> !token.isExpired(Instant.now()))
                .map(token -> decryptSafely(token).map(plain -> new InviteTokenResponseDTO(plain, token.getExpiresAt())))
                .flatMap(optional -> optional)
                .orElseGet(() -> issue(clubId));
    }

    public InviteTokenResponseDTO issue(UUID clubId) {
        String currentPlain = clubInviteTokenRepository.findByClubId(clubId)
                .flatMap(this::decryptSafely)
                .orElse(null);

        String token;
        do {
            token = generateToken();
        } while (token.equals(currentPlain));

        String cipherText = cipher.encrypt(token);
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);

        clubInviteTokenRepository.upsert(clubId, cipherText, expiresAt, Instant.now());
        log.info("Token de convite gerado - clubId: {}", clubId);
        return new InviteTokenResponseDTO(token, expiresAt);
    }

    public void validate(UUID clubId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "invite token required");
        }

        ClubInviteToken stored = clubInviteTokenRepository.findByClubId(clubId).orElse(null);
        String plain = stored == null || stored.isExpired(Instant.now())
                ? null
                : decryptSafely(stored).orElse(null);

        if (plain == null || !MessageDigest.isEqual(plain.getBytes(StandardCharsets.UTF_8), rawToken.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Token de convite invalido ou expirado - clubId: {}", clubId);
            throw new ResponseStatusException(BAD_REQUEST, "invalid or expired invite token");
        }
    }

    @Scheduled(fixedRate = 3_600_000)
    public void cleanExpiredTokens() {
        int deleted = clubInviteTokenRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Limpeza de tokens de convite expirados - removidos: {}", deleted);
        }
    }

    private Optional<String> decryptSafely(ClubInviteToken token) {
        try {
            return Optional.of(cipher.decrypt(token.getTokenCipher()));
        } catch (RuntimeException e) {
            log.warn("Falha ao descriptografar token de convite - clubId: {}", token.getClubId());
            return Optional.empty();
        }
    }

    private static String generateToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(TOKEN_ALPHABET.charAt(SECURE_RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return sb.toString();
    }
}
