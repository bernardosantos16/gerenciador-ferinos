package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.ClubInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ClubInviteTokenRepository extends JpaRepository<ClubInviteToken, Long> {

    Optional<ClubInviteToken> findByClubId(UUID clubId);

    @Modifying
    @Query(value = """
            INSERT INTO club_invite_tokens (club_id, token_cipher, expires_at, created_at)
            VALUES (:clubId, :cipher, :expiresAt, :createdAt)
            ON CONFLICT (club_id) DO UPDATE
            SET token_cipher = EXCLUDED.token_cipher,
                expires_at = EXCLUDED.expires_at,
                created_at = EXCLUDED.created_at
            """, nativeQuery = true)
    void upsert(@Param("clubId") UUID clubId,
                @Param("cipher") String cipher,
                @Param("expiresAt") Instant expiresAt,
                @Param("createdAt") Instant createdAt);

    @Modifying(flushAutomatically = true)
    @Query("delete from ClubInviteToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
