package com.bernardo.geradortimes.user.model;

import com.bernardo.geradortimes.shared.enums.TokenType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
@Getter
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "user_id")
    private UUID userId;

    protected VerificationToken() {
    }

    private VerificationToken(String tokenHash, TokenType type, Instant expiresAt, UUID userId) {
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.userId = userId;
    }

    public static VerificationToken create(String tokenHash, TokenType type, Instant expiresAt, UUID userId) {
        return new VerificationToken(tokenHash, type, expiresAt, userId);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
