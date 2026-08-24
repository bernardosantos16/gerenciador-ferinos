package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.ClubMembershipRequest;
import com.bernardo.geradortimes.shared.enums.MembershipRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClubMembershipRequestRepository extends JpaRepository<ClubMembershipRequest, Long> {

    boolean existsByClubIdAndUserIdAndStatus(UUID clubId, UUID userId, MembershipRequestStatus status);

    Page<ClubMembershipRequest> findByClubIdAndStatus(UUID clubId, MembershipRequestStatus status, Pageable pageable);

    Optional<ClubMembershipRequest> findFirstByClubIdAndUserIdOrderByRequestedAtDesc(UUID clubId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ClubMembershipRequest r where r.id = :id")
    Optional<ClubMembershipRequest> findByIdForUpdate(@Param("id") Long id);
}
