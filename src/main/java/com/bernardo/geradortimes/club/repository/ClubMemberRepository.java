package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    boolean existsByClubIdAndUserId(UUID clubId, UUID userId);
    boolean existsByClubIdAndUserIdAndClubRole(UUID clubId, UUID userId, ClubRole clubRole);
    List<ClubMember> findByClubIdAndIdIn(UUID clubId, List<Long> ids);
    Page<ClubMember> findByClubId(UUID clubId, Pageable pageable);
}
