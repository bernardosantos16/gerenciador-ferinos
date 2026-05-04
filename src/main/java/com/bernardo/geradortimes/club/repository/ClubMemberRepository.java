package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    boolean existsByClubIdAndUserId(UUID clubId, UUID userId);
    boolean existsByClubIdAndUserIdAndClubRole(UUID clubId, UUID userId, ClubRole clubRole);
    List<ClubMember> findByClubIdAndIdIn(UUID clubId, List<Long> ids);
    Page<ClubMember> findByClubId(UUID clubId, Pageable pageable);

    @Query(
        """
        SELECT c FROM Club c
        INNER JOIN ClubMember cm
        ON c.id = cm.clubId
        WHERE cm.userId = :userId AND cm.clubRole = :clubRole
        """
    )
    List<Club> findByUserIdAndClubRole(UUID userId, ClubRole clubRole);
}
