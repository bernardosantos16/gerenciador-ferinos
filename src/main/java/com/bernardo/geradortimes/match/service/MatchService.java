package com.bernardo.geradortimes.match.service;

import com.bernardo.geradortimes.club.service.ClubAuthorizationService;
import com.bernardo.geradortimes.match.dto.request.CreateMatchRequestDTO;
import com.bernardo.geradortimes.match.dto.response.MatchParticipantResponseDTO;
import com.bernardo.geradortimes.match.dto.response.MatchResponseDTO;
import com.bernardo.geradortimes.match.model.Match;
import com.bernardo.geradortimes.match.model.MatchParticipant;
import com.bernardo.geradortimes.match.repository.MatchParticipantRepository;
import com.bernardo.geradortimes.match.repository.MatchRepository;
import com.bernardo.geradortimes.team.repository.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final TeamRepository teamRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public MatchService(
            MatchRepository matchRepository,
            MatchParticipantRepository matchParticipantRepository,
            TeamRepository teamRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.teamRepository = teamRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    public MatchResponseDTO create(CreateMatchRequestDTO request) {
        clubAuthorizationService.requireDirector(request.clubId());
        Match match = Match.create(request.clubId(), request.dateTime());
        Match saved = matchRepository.save(match);
        log.info("nova partida {} salva", saved.getId());
        return toResponse(saved);

    }

    @Transactional(readOnly = true)
    public MatchResponseDTO getById(UUID id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireMember(match.getClubId());
        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDTO> listByClub(UUID clubId) {
        clubAuthorizationService.requireMember(clubId);
        return matchRepository.findByClubId(clubId).stream().map(MatchService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchParticipantResponseDTO> listParticipants(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireMember(match.getClubId());
        return matchParticipantRepository.findByMatchId(matchId).stream().map(MatchService::toResponse).toList();
    }

    public void delete(UUID id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireDirector(match.getClubId());

        // Clean up dependent data first.
        matchParticipantRepository.deleteByMatchId(id);
        teamRepository.deleteByMatchId(id);

        matchRepository.delete(match);

        log.info("partida deletada");
    }

    private static MatchResponseDTO toResponse(Match match) {
        return new MatchResponseDTO(match.getId(), match.getClubId(), match.getDateTime());
    }

    private static MatchParticipantResponseDTO toResponse(MatchParticipant participant) {
        return new MatchParticipantResponseDTO(
                participant.getId(),
                participant.getMatchId(),
                participant.getClubMemberId(),
                participant.getPosition(),
                participant.getTeamId()
        );
    }
}

