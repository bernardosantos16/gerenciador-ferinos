package com.bernardo.geradortimes.team.repository;

import com.bernardo.geradortimes.team.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByMatchId(UUID matchId);

    Page<Team> findByMatchId(UUID matchId, Pageable pageable);

    boolean existsByIdAndMatchId(Long id, UUID matchId);

    void deleteByMatchId(UUID matchId);
}
