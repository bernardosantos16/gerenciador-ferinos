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

    @Column(name = "token_salt", length = 64)
    @Setter
    private String tokenSalt;

    protected VerificationToken() {
    }

    private VerificationToken(String tokenHash, TokenType type, Instant expiresAt, String email, String tokenSalt) {
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.email = email;
        this.tokenSalt = tokenSalt;
    }

    public static VerificationToken create(String tokenHash, TokenType type, Instant expiresAt, String email, String tokenSalt) {
        return new VerificationToken(tokenHash, type, expiresAt, email, tokenSalt);
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
