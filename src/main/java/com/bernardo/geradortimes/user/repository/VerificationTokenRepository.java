package com.bernardo.geradortimes.user.repository;

import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.user.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHashAndTypeAndEmail(String tokenHash, TokenType type, String email);

    @Query("""
        SELECT v FROM VerificationToken v
        WHERE v.email = :email
          AND v.type = :type
          AND v.usedAt IS NULL
          AND v.expiresAt > :now
        """)
    Optional<VerificationToken> findActiveByEmailAndType(String email, TokenType type, Instant now);

    void deleteByExpiresAtBeforeOrUsedAtIsNotNull(Instant now);

}
