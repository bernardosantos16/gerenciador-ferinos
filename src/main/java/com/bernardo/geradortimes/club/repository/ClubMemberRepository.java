package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    boolean existsByClubIdAndUserId(UUID clubId, UUID userId);
    boolean existsByClubIdAndUserIdAndClubRole(UUID clubId, UUID userId, ClubRole clubRole);
    List<ClubMember> findByClubIdAndIdIn(UUID clubId, Collection<Long> ids);
    Page<ClubMember> findByClubId(UUID clubId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cm from ClubMember cm where cm.id = :id")
    Optional<ClubMember> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query(
            """
            update ClubMember cm
            set cm.timesChampion =
                case
                    when coalesce(cm.timesChampion, 0) + :delta < 0 then 0
                    else coalesce(cm.timesChampion, 0) + :delta
                end
            where cm.clubId = :clubId and cm.id in :memberIds
            """
    )
    int incrementTimesChampion(
            @Param("clubId") UUID clubId,
            @Param("memberIds") Collection<Long> memberIds,
            @Param("delta") int delta
    );

    @Modifying(flushAutomatically = true)
    @Query(
            """
            update ClubMember cm
            set cm.timesMvp =
                case
                    when coalesce(cm.timesMvp, 0) + :delta < 0 then 0
                    else coalesce(cm.timesMvp, 0) + :delta
                end
            where cm.clubId = :clubId and cm.id in :memberIds
            """
    )
    int incrementTimesMvp(
            @Param("clubId") UUID clubId,
            @Param("memberIds") Collection<Long> memberIds,
            @Param("delta") int delta
    );

    @Query(
        """
        SELECT c FROM Club c
        INNER JOIN ClubMember cm
        ON c.id = cm.clubId
        WHERE cm.userId = :userId AND cm.clubRole = :clubRole AND c.status = 'ACTIVE'
        """
    )
    List<Club> findByUserIdAndClubRole(UUID userId, ClubRole clubRole);
}
