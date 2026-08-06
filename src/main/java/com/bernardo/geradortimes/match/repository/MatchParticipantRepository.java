package com.bernardo.geradortimes.match.repository;

import com.bernardo.geradortimes.match.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    List<MatchParticipant> findByMatchId(UUID matchId);

    List<MatchParticipant> findByMatchIdOrderBySortOrder(UUID matchId);

    List<MatchParticipant> findByMatchIdAndTeamId(UUID matchId, Long teamId);

    Optional<MatchParticipant> findByMatchIdAndClubMemberId(UUID matchId, Long clubMemberId);

    List<MatchParticipant> findByClubMemberIdIn(List<Long> clubMemberIds);

    @Modifying
    @Query("DELETE FROM MatchParticipant mp WHERE mp.matchId = :matchId")
    void deleteByMatchId(@Param("matchId") UUID matchId);
}
