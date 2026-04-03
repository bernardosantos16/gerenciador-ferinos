package com.bernardo.geradortimes.match.repository;

import com.bernardo.geradortimes.match.model.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByClubId(UUID clubId);

    Page<Match> findByClubId(UUID clubId, Pageable pageable);
}
