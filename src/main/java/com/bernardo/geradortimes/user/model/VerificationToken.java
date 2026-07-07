package com.bernardo.geradortimes.user.model;

import com.bernardo.geradortimes.shared.enums.TokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "verification_tokens")
@Getter
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64)
    @Setter
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    @Setter
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(nullable = false, length = 100)
    private String email;

    protected VerificationToken() {
    }

    private VerificationToken(String tokenHash, TokenType type, Instant expiresAt, String email) {
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.email = email;
    }

    public static VerificationToken create(String tokenHash, TokenType type, Instant expiresAt, String email) {
        return new VerificationToken(tokenHash, type, expiresAt, email);
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
