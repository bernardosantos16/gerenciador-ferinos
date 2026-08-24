package com.bernardo.geradortimes.club.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "club_invite_tokens")
@Getter
public class ClubInviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private UUID clubId;

    @Column(name = "token_cipher", nullable = false, length = 512)
    private String tokenCipher;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClubInviteToken() {}

    private ClubInviteToken(UUID clubId, String tokenCipher, Instant expiresAt) {
        this.clubId = clubId;
        this.tokenCipher = tokenCipher;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static ClubInviteToken create(UUID clubId, String tokenCipher, Instant expiresAt) {
        return new ClubInviteToken(clubId, tokenCipher, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void update(String tokenCipher, Instant expiresAt) {
        this.tokenCipher = tokenCipher;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
}
