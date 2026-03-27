package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.ClubJersey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClubJerseyRepository extends JpaRepository<ClubJersey, Long> {
    List<ClubJersey> findByClubId(UUID clubId);
}
