package com.bernardo.geradortimes.user.repository;

import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.user.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, TokenType type);

    @Query("""
        SELECT v FROM VerificationToken v
        WHERE v.userId = :userId
          AND v.type = :type
          AND v.usedAt IS NULL
          AND v.expiresAt > :now
        """)
    Optional<VerificationToken> findActiveByUserIdAndType(UUID userId, TokenType type, Instant now);

}
