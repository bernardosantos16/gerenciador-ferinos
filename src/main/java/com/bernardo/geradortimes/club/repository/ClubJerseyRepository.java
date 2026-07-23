package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.model.ClubJersey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClubJerseyRepository extends JpaRepository<ClubJersey, Long> {

    Page<ClubJersey> findByClubId(UUID clubId, Pageable pageable);
}
