package com.bernardo.geradortimes.club.repository;

import com.bernardo.geradortimes.club.dto.response.ClubResponseDTO;
import com.bernardo.geradortimes.club.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<Club, UUID> {
    Optional<Club> findByNicknameValue(String nickname);

    boolean existsByNicknameValue(String nickname);

    boolean existsByNicknameValueAndIdNot(String nickname, UUID id);
}
