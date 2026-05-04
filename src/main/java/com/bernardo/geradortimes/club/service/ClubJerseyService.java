package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.club.dto.request.AddJerseyRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateJerseyRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubJerseyResponseDTO;
import com.bernardo.geradortimes.club.model.ClubJersey;
import com.bernardo.geradortimes.club.repository.ClubJerseyRepository;
import com.bernardo.geradortimes.shared.value_object.HexColor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ClubJerseyService {

    private final ClubJerseyRepository clubJerseyRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public ClubJerseyService(ClubJerseyRepository clubJerseyRepository, ClubAuthorizationService clubAuthorizationService) {
        this.clubJerseyRepository = clubJerseyRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    public ClubJerseyResponseDTO addJersey(UUID clubId, AddJerseyRequestDTO requestDTO) {
        clubAuthorizationService.requireDirector(clubId);
        ClubJersey clubJersey = ClubJersey.create(
                HexColor.of(requestDTO.hexColor()),
                requestDTO.name().trim(),
                requestDTO.isGoalkeeperJersey(),
                clubId
        );
        ClubJersey saved = clubJerseyRepository.save(clubJersey);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ClubJerseyResponseDTO> pageByClub(UUID clubId, Pageable pageable) {
        clubAuthorizationService.requireMember(clubId);
        return clubJerseyRepository.findByClubId(clubId, pageable).map(ClubJerseyService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ClubJerseyResponseDTO> listByClub(UUID clubId) {
        clubAuthorizationService.requireMember(clubId);
        return clubJerseyRepository.findByClubId(clubId)
                .stream()
                .map(ClubJerseyService::toResponse)
                .toList();
    }

    public void delete(UUID clubId, Long jerseyId) {
        clubAuthorizationService.requireDirector(clubId);
        ClubJersey jersey = clubJerseyRepository.findById(jerseyId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "jersey not found"));
        if (!clubId.equals(jersey.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "jersey not found");
        }
        clubJerseyRepository.delete(jersey);
    }

    public ClubJerseyResponseDTO updateJersey(UUID clubId, Long jerseyId, UpdateJerseyRequestDTO request) {
        clubAuthorizationService.requireDirector(clubId);
        ClubJersey jersey = clubJerseyRepository.findById(jerseyId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "jersey not found"));
        if (!clubId.equals(jersey.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "jersey not found");
        }

        if (request.name() != null && !request.name().isBlank()) {
            jersey.changeName(request.name());
        }
        if (request.hexColor() != null && !request.hexColor().isBlank()) {
            jersey.changeColor(HexColor.of(request.hexColor()));
        }
        if (request.isGoalkeeperJersey() != null) {
            jersey.changeIsGoalkeeper(request.isGoalkeeperJersey());
        }

        ClubJersey saved = clubJerseyRepository.save(jersey);
        return toResponse(saved);
    }

    private static ClubJerseyResponseDTO toResponse(ClubJersey jersey) {
        return new ClubJerseyResponseDTO(
                jersey.getId(),
                jersey.getName(),
                jersey.getHexColor().getValue(),
                jersey.getIsGoalkeeperJersey(),
                jersey.getClubId()
        );
    }
}
