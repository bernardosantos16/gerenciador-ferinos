package com.bernardo.geradortimes.match.service;

import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.club.service.ClubAuthorizationService;
import com.bernardo.geradortimes.match.dto.request.CreateMatchBatchRequestDTO;
import com.bernardo.geradortimes.match.dto.request.CreateMatchRequestDTO;
import com.bernardo.geradortimes.match.dto.request.SetMatchResultRequestDTO;
import com.bernardo.geradortimes.match.dto.request.UpdateMatchRequestDTO;
import com.bernardo.geradortimes.match.dto.response.MatchParticipantResponseDTO;
import com.bernardo.geradortimes.match.dto.response.MatchResponseDTO;
import com.bernardo.geradortimes.match.model.Match;
import com.bernardo.geradortimes.match.model.MatchParticipant;
import com.bernardo.geradortimes.match.repository.MatchParticipantRepository;
import com.bernardo.geradortimes.match.repository.MatchRepository;
import com.bernardo.geradortimes.team.model.Team;
import com.bernardo.geradortimes.team.repository.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final TeamRepository teamRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public MatchService(
            MatchRepository matchRepository,
            MatchParticipantRepository matchParticipantRepository,
            TeamRepository teamRepository,
            ClubMemberRepository clubMemberRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.teamRepository = teamRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    public MatchResponseDTO create(CreateMatchRequestDTO request) {
        clubAuthorizationService.requireDirector(request.clubId());
        Match match = Match.create(request.clubId(), request.dateTime());
        Match saved = matchRepository.save(match);
        log.info("nova partida {} salva", saved.getId());
        return toResponse(saved);

    }

    public List<MatchResponseDTO> createBatch(CreateMatchBatchRequestDTO request) {
        clubAuthorizationService.requireDirector(request.clubId());
        LocalDate todayInClubZone = LocalDate.now(request.zoneId());

        if (request.startDate().isBefore(todayInClubZone)) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate must be today or in the future");
        }
        if (request.endDate().isBefore(todayInClubZone)) {
            throw new ResponseStatusException(BAD_REQUEST, "endDate must be today or in the future");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "endDate must be after or equal to startDate");
        }

        long daysBetween = request.startDate().until(request.endDate()).getDays();
        if (daysBetween > 90) {
            throw new ResponseStatusException(BAD_REQUEST, "date range cannot exceed 90 days");
        }

        List<Match> matches = new ArrayList<>();
        LocalDate current = request.startDate();
        while (!current.isAfter(request.endDate())) {
            if (current.getDayOfWeek() == request.dayOfWeek()) {
                Instant matchDateTime = current.atTime(request.time()).atZone(request.zoneId()).toInstant();
                matches.add(Match.create(request.clubId(), matchDateTime));
            }
            current = current.plusDays(1);
        }

        if (matches.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "no dates found matching the selected day of week in the given range");
        }

        List<Match> saved = matchRepository.saveAll(matches);
        log.info("{} partidas criadas em lote para o clube {}", saved.size(), request.clubId());
        return saved.stream().map(MatchService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MatchResponseDTO getById(UUID id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireMember(match.getClubId());
        return toResponse(match);
    }

    @Transactional(readOnly = true)
    public Page<MatchResponseDTO> listByClub(UUID clubId, Pageable pageable) {
        clubAuthorizationService.requireMember(clubId);
        return matchRepository.findByClubId(clubId, pageable).map(MatchService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MatchResponseDTO> listByClubAndUpcoming(UUID clubId, Pageable pageable) {
        clubAuthorizationService.requireMember(clubId);
        return matchRepository.findByClubIdAndDateTimeAfter(clubId, Instant.now(), pageable)
                .map(MatchService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MatchParticipantResponseDTO> listParticipants(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireMember(match.getClubId());
        return matchParticipantRepository.findByMatchId(matchId).stream().map(MatchService::toResponse).toList();
    }

    public MatchResponseDTO update(UUID id, UpdateMatchRequestDTO request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireDirector(match.getClubId());
        if (match.hasResult()) {
            throw new ResponseStatusException(BAD_REQUEST, "match result already set");
        }

        match.updateDateTime(request.dateTime());
        Match saved = matchRepository.save(match);
        log.info("partida atualizada - matchId: {}", saved.getId());
        return toResponse(saved);
    }

    public MatchResponseDTO setResult(UUID id, SetMatchResultRequestDTO request) {
        Match match = matchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);

        validateMatchAlreadyPlayed(match);
        validateTeamInMatch(match.getId(), request.teamChampionId());

        List<MatchParticipant> championParticipants = matchParticipantRepository.findByMatchIdAndTeamId(
                match.getId(),
                request.teamChampionId()
        );
        if (championParticipants.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "champion team has no participants");
        }

        MatchParticipant mvpParticipant = matchParticipantRepository
                .findByMatchIdAndClubMemberId(match.getId(), request.clubMemberMvpId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "mvp member is not a participant in this match"));
        validateMvpParticipantAssignedToMatchTeam(match.getId(), mvpParticipant);

        Set<Long> selectedMemberIds = new HashSet<>();
        championParticipants.forEach(participant -> selectedMemberIds.add(participant.getClubMemberId()));
        selectedMemberIds.add(request.clubMemberMvpId());
        validateMembersBelongToClub(match.getClubId(), selectedMemberIds);

        Map<Long, Integer> championDeltas = buildChampionDeltas(match, request.teamChampionId(), championParticipants);
        Map<Long, Integer> mvpDeltas = buildMvpDeltas(match, request.clubMemberMvpId());

        applyDeltas(match.getClubId(), championDeltas, clubMemberRepository::incrementTimesChampion);
        applyDeltas(match.getClubId(), mvpDeltas, clubMemberRepository::incrementTimesMvp);

        match.setResult(request.teamChampionId(), request.clubMemberMvpId());
        Match saved = matchRepository.save(match);
        log.info(
                "resultado da partida matchId={} teamChampionId={} clubMemberMvpId={} confirmado",
                saved.getId(),
                saved.getTeamChampionId(),
                saved.getClubMemberMvpId()
        );
        return toResponse(saved);
    }

    public void delete(UUID id) {
        Match match = matchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);

        // Clean up dependent data first.
        matchParticipantRepository.deleteByMatchId(id);
        teamRepository.deleteByMatchId(id);

        matchRepository.delete(match);

        log.info("partida deletada - matchId: {}", id);
    }

    private static MatchResponseDTO toResponse(Match match) {
        return new MatchResponseDTO(
                match.getId(),
                match.getClubId(),
                match.getDateTime(),
                match.getTeamChampionId(),
                match.getClubMemberMvpId()
        );
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

    private static void validateMatchAlreadyPlayed(Match match) {
        if (!match.getDateTime().isBefore(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "match must be in the past to set result");
        }
    }

    private static void ensureMatchResultNotSet(Match match) {
        if (match.hasResult()) {
            throw new ResponseStatusException(BAD_REQUEST, "match result already set");
        }
    }

    private void validateTeamInMatch(UUID matchId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "champion team is not in this match"));
        if (!matchId.equals(team.getMatchId())) {
            throw new ResponseStatusException(BAD_REQUEST, "champion team is not in this match");
        }
    }

    private void validateMvpParticipantAssignedToMatchTeam(UUID matchId, MatchParticipant participant) {
        if (participant.getTeamId() == null || !teamRepository.existsByIdAndMatchId(participant.getTeamId(), matchId)) {
            throw new ResponseStatusException(BAD_REQUEST, "mvp member is not assigned to a team in this match");
        }
    }

    private void validateMembersBelongToClub(UUID clubId, Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }

        List<ClubMember> members = clubMemberRepository.findByClubIdAndIdIn(clubId, memberIds);
        Set<Long> foundIds = new HashSet<>();
        members.forEach(member -> foundIds.add(member.getId()));
        if (foundIds.size() != memberIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "all selected participants must belong to the match club");
        }
    }

    private Map<Long, Integer> buildChampionDeltas(
            Match match,
            Long newChampionTeamId,
            List<MatchParticipant> newChampionParticipants
    ) {
        Map<Long, Integer> deltas = new HashMap<>();
        Long previousChampionTeamId = match.getTeamChampionId();
        if (Objects.equals(previousChampionTeamId, newChampionTeamId)) {
            return deltas;
        }

        if (previousChampionTeamId != null) {
            matchParticipantRepository.findByMatchIdAndTeamId(match.getId(), previousChampionTeamId)
                    .forEach(participant -> addDelta(deltas, participant.getClubMemberId(), -1));
        }

        newChampionParticipants.forEach(participant -> addDelta(deltas, participant.getClubMemberId(), 1));
        return deltas;
    }

    private static Map<Long, Integer> buildMvpDeltas(Match match, Long newMvpMemberId) {
        Map<Long, Integer> deltas = new HashMap<>();
        Long previousMvpMemberId = match.getClubMemberMvpId();
        if (Objects.equals(previousMvpMemberId, newMvpMemberId)) {
            return deltas;
        }

        if (previousMvpMemberId != null) {
            addDelta(deltas, previousMvpMemberId, -1);
        }
        addDelta(deltas, newMvpMemberId, 1);
        return deltas;
    }

    private void reverseResultCounters(Match match) {
        Map<Long, Integer> championDeltas = new HashMap<>();
        if (match.getTeamChampionId() != null) {
            matchParticipantRepository.findByMatchIdAndTeamId(match.getId(), match.getTeamChampionId())
                    .forEach(participant -> addDelta(championDeltas, participant.getClubMemberId(), -1));
        }

        Map<Long, Integer> mvpDeltas = new HashMap<>();
        if (match.getClubMemberMvpId() != null) {
            addDelta(mvpDeltas, match.getClubMemberMvpId(), -1);
        }

        applyDeltas(match.getClubId(), championDeltas, clubMemberRepository::incrementTimesChampion);
        applyDeltas(match.getClubId(), mvpDeltas, clubMemberRepository::incrementTimesMvp);
    }

    private static void addDelta(Map<Long, Integer> deltas, Long memberId, int delta) {
        deltas.merge(memberId, delta, Integer::sum);
        if (deltas.get(memberId) == 0) {
            deltas.remove(memberId);
        }
    }

    private static void applyDeltas(UUID clubId, Map<Long, Integer> deltas, StatIncrementer incrementer) {
        if (deltas.isEmpty()) {
            return;
        }

        Map<Integer, List<Long>> memberIdsByDelta = new HashMap<>();
        deltas.forEach((memberId, delta) -> memberIdsByDelta
                .computeIfAbsent(delta, ignored -> new ArrayList<>())
                .add(memberId));

        memberIdsByDelta.forEach((delta, memberIds) -> incrementer.increment(clubId, memberIds, delta));
    }

    @FunctionalInterface
    private interface StatIncrementer {
        void increment(UUID clubId, Collection<Long> memberIds, int delta);
    }
}
