package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    boolean existsByClubIdAndUserId(UUID clubId, UUID userId);
    boolean existsByClubIdAndUserIdAndClubRole(UUID clubId, UUID userId, ClubRole clubRole);
}
