package com.bernardo.geradortimes.match.repository;

import com.bernardo.geradortimes.match.model.Match;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByClubId(UUID clubId);

    Page<Match> findByClubId(UUID clubId, Pageable pageable);

    Page<Match> findByClubIdAndDateTimeAfter(UUID clubId, Instant dateTime, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :id")
    Optional<Match> findByIdForUpdate(UUID id);
}
