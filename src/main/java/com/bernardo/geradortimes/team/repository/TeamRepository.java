package com.bernardo.geradortimes.team.repository;

import com.bernardo.geradortimes.team.model.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByMatchId(UUID matchId);

    Page<Team> findByMatchId(UUID matchId, Pageable pageable);

    boolean existsByIdAndMatchId(Long id, UUID matchId);

    @Modifying
    @Query("DELETE FROM Team t WHERE t.matchId = :matchId")
    void deleteByMatchId(@Param("matchId") UUID matchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Team t where t.id = :id")
    Optional<Team> findByIdForUpdate(@Param("id") Long id);
}
