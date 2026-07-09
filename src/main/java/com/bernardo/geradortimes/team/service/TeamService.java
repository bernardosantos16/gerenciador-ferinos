package com.bernardo.geradortimes.team.service;

import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.model.ClubJersey;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.club.repository.ClubJerseyRepository;
import com.bernardo.geradortimes.club.service.ClubAuthorizationService;
import com.bernardo.geradortimes.shared.enums.MatchParticipantPosition;
import com.bernardo.geradortimes.match.model.Match;
import com.bernardo.geradortimes.match.model.MatchParticipant;
import com.bernardo.geradortimes.match.repository.MatchParticipantRepository;
import com.bernardo.geradortimes.match.repository.MatchRepository;
import com.bernardo.geradortimes.team.dto.request.CreateTeamRequestDTO;
import com.bernardo.geradortimes.team.dto.request.GenerateTeamsRequestDTO;
import com.bernardo.geradortimes.team.dto.request.UpdateTeamJerseyRequestDTO;
import com.bernardo.geradortimes.team.dto.request.UpdateTeamRequestDTO;
import com.bernardo.geradortimes.team.dto.request.SwapPlayersRequestDTO;
import com.bernardo.geradortimes.team.dto.request.PlayerSwapDTO;
import com.bernardo.geradortimes.team.dto.response.GenerateTeamsResponseDTO;
import com.bernardo.geradortimes.team.dto.response.GeneratedTeamDTO;
import com.bernardo.geradortimes.team.dto.response.TeamResponseDTO;
import com.bernardo.geradortimes.team.model.Team;
import com.bernardo.geradortimes.team.repository.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@Transactional
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubJerseyRepository clubJerseyRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchRepository matchRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public TeamService(
            TeamRepository teamRepository,
            ClubMemberRepository clubMemberRepository,
            ClubJerseyRepository clubJerseyRepository,
            MatchParticipantRepository matchParticipantRepository,
            MatchRepository matchRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.teamRepository = teamRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.clubJerseyRepository = clubJerseyRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.matchRepository = matchRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    public TeamResponseDTO create(CreateTeamRequestDTO request) {
        Match match = requireMatch(request.matchId());
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);
        validateJersey(match.getClubId(), request.clubJerseyId());
        Team team = Team.create(request.matchId(), request.clubJerseyId());
        Team saved = teamRepository.save(team);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TeamResponseDTO getById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "team not found"));
        Match match = requireMatch(team.getMatchId());
        clubAuthorizationService.requireMember(match.getClubId());
        return toResponse(team);
    }

    @Transactional(readOnly = true)
    public Page<TeamResponseDTO> list(UUID matchId, Pageable pageable) {
        if (matchId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "matchId is required");
        }
        Match match = requireMatch(matchId);
        clubAuthorizationService.requireMember(match.getClubId());
        return teamRepository.findByMatchId(matchId, pageable).map(TeamService::toResponse);
    }

    public TeamResponseDTO updateJersey(Long id, UpdateTeamJerseyRequestDTO request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "team not found"));
        Match match = requireMatch(team.getMatchId());
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);
        validateJersey(match.getClubId(), request.clubJerseyId());
        team.changeJersey(request.clubJerseyId());
        return toResponse(team);
    }

    public TeamResponseDTO update(Long id, UpdateTeamRequestDTO request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "team not found"));

        Match match = requireMatch(team.getMatchId());
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);
        validateJersey(match.getClubId(), request.clubJerseyId());
        team.changeJersey(request.clubJerseyId());

        Team saved = teamRepository.save(team);
        return toResponse(saved);
    }

    public GenerateTeamsResponseDTO generate(GenerateTeamsRequestDTO request) {
        UUID matchId = request.matchId();
        List<Long> lineIds = request.lineMemberIds() == null ? List.of() : request.lineMemberIds();
        List<Long> goalkeeperIds = request.goalkeeperMemberIds() == null ? List.of() : request.goalkeeperMemberIds();
        int maxLinePlayers = request.maxLinePlayers() == null ? 0 : request.maxLinePlayers();
        log.info(
                "geracao times requisitada matchId={} lineIds={} goalkeeperIds={} maxLinePlayers={}",
                matchId,
                lineIds.size(),
                goalkeeperIds.size(),
                maxLinePlayers
        );

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "match not found: " + matchId));
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);

        if (maxLinePlayers < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "maxLinePlayers must be >= 1");
        }
        if (lineIds.size() < 2) {
            throw new ResponseStatusException(BAD_REQUEST, "lineMemberIds must contain at least 2 members");
        }

        List<Long> normalizedLineIds = normalizeIds(lineIds, "lineMemberIds");
        List<Long> normalizedGoalkeeperIds = normalizeIds(goalkeeperIds, "goalkeeperMemberIds");

        Set<Long> overlap = new HashSet<>(normalizedLineIds);
        overlap.retainAll(normalizedGoalkeeperIds);
        if (!overlap.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "a member cannot be both LINE and GOAL for the same match");
        }

        Map<Long, ClubMember> membersById = loadMembersById(match.getClubId(), union(normalizedLineIds, normalizedGoalkeeperIds));
        List<ClubMember> lineMembers = normalizedLineIds.stream().map(membersById::get).toList();
        List<ClubMember> goalkeeperMembers = normalizedGoalkeeperIds.stream().map(membersById::get).toList();

        int teamCount = computeTeamCount(lineMembers.size(), maxLinePlayers);
        List<Integer> teamSizes = computeTeamSizes(lineMembers.size(), teamCount);

        // Replace any previously generated data for this match.
        matchParticipantRepository.deleteByMatchId(matchId);
        teamRepository.deleteByMatchId(matchId);

        List<Team> teams = new ArrayList<>(teamCount);
        for (int i = 0; i < teamCount; i++) {
            teams.add(Team.create(matchId, null));
        }
        List<Team> savedTeams = teamRepository.saveAll(teams);

        List<ScoredMember> scoredLineMembers = scoreMembers(lineMembers);
        scoredLineMembers.sort(Comparator
                .comparingDouble(ScoredMember::score).reversed()
                .thenComparingLong(ScoredMember::memberId));

        List<TeamBucket> buckets = new ArrayList<>(teamCount);
        for (int i = 0; i < teamCount; i++) {
            buckets.add(new TeamBucket(savedTeams.get(i).getId(), teamSizes.get(i)));
        }

        distributeLinePlayers(scoredLineMembers, buckets);

        List<Long> unassignedGoalkeepers = new ArrayList<>();

        if (goalkeeperMembers.size() == teamCount) {
            List<ScoredMember> scoredKeepers = new ArrayList<>(scoreMembers(goalkeeperMembers));
            scoredKeepers.sort(Comparator
                    .comparingDouble(ScoredMember::score).reversed()
                    .thenComparingLong(ScoredMember::memberId));

            // Assign strongest keepers to weakest teams without a goalkeeper (1 per team).
            for (ScoredMember keeper : scoredKeepers) {
                TeamBucket weakest = buckets.stream()
                        .filter(b -> b.goalkeeperMemberId == null)
                        .min(Comparator.comparingDouble(TeamBucket::totalScore)
                                .thenComparingLong(b -> b.teamId))
                        .orElse(null);

                if (weakest != null) {
                    weakest.assignGoalkeeper(keeper);
                } else {
                    // This shouldn't happen if sizes match
                    unassignedGoalkeepers.add(keeper.memberId());
                }
            }
        } else {
            // All goalkeepers unassigned
            unassignedGoalkeepers.addAll(goalkeeperMembers.stream().map(ClubMember::getId).toList());
        }

        // Persist participants (confirmation + team assignment if applicable).
        List<MatchParticipant> participants = new ArrayList<>(
                lineMembers.size() + goalkeeperMembers.size()
        );

        for (TeamBucket b : buckets) {
            for (ScoredMember m : b.lineMembers) {
                participants.add(MatchParticipant.create(
                        matchId,
                        m.memberId,
                        MatchParticipantPosition.LINE,
                        b.teamId
                ));
            }
            if (b.goalkeeperMemberId != null) {
                participants.add(MatchParticipant.create(
                        matchId,
                        b.goalkeeperMemberId,
                        MatchParticipantPosition.GOAL,
                        b.teamId
                ));
            }
        }

        for (Long gkId : unassignedGoalkeepers) {
            participants.add(MatchParticipant.create(
                    matchId,
                    gkId,
                    MatchParticipantPosition.GOAL,
                    null
            ));
        }

        matchParticipantRepository.saveAll(participants);

        for (TeamBucket b : buckets) {
            savedTeams.stream()
                    .filter(t -> t.getId().equals(b.teamId))
                    .findFirst()
                    .ifPresent(t -> t.changeScore(b.totalScore()));
        }
        teamRepository.saveAll(savedTeams);

        List<GeneratedTeamDTO> generatedTeams = buckets.stream()
                .map(b -> new GeneratedTeamDTO(
                        b.teamId,
                        b.lineMembers.stream().map(x -> x.memberId).toList(),
                        b.goalkeeperMemberId,
                        b.totalScore()
                ))
                .toList();

        log.info(
                "geracao times concluida matchId={} teamCount={} unassignedGoalkeepers={}",
                matchId,
                teamCount,
                unassignedGoalkeepers.size()
        );
        return new GenerateTeamsResponseDTO(matchId, teamCount, generatedTeams, unassignedGoalkeepers);
    }

    public void swapPlayers(SwapPlayersRequestDTO request) {
        UUID matchId = request.matchId();
        List<PlayerSwapDTO> swaps = request.swaps();

        if (swaps == null || swaps.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "swaps list cannot be empty");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);

        // Validate that teams have been generated for this match
        List<Team> teams = teamRepository.findByMatchId(matchId);
        if (teams.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "teams must be generated before swapping players");
        }

        // Get all participants for this match
        List<MatchParticipant> allParticipants = matchParticipantRepository.findByMatchId(matchId);
        if (allParticipants.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "no participants found for this match");
        }

        // Create a map of member IDs to their participants
        Map<Long, MatchParticipant> participantsByMemberId = new HashMap<>();
        for (MatchParticipant p : allParticipants) {
            participantsByMemberId.put(p.getClubMemberId(), p);
        }

        // Collect all member IDs from swaps
        Set<Long> memberIds = new HashSet<>();
        for (PlayerSwapDTO swap : swaps) {
            memberIds.add(swap.memberIdFrom());
            memberIds.add(swap.memberIdTo());
        }

        // Validate that all members are participants in this match
        for (Long memberId : memberIds) {
            if (!participantsByMemberId.containsKey(memberId)) {
                throw new ResponseStatusException(BAD_REQUEST, "member " + memberId + " is not a participant in this match");
            }
        }

        // Validate and perform swaps
        for (PlayerSwapDTO swap : swaps) {
            MatchParticipant p1 = participantsByMemberId.get(swap.memberIdFrom());
            MatchParticipant p2 = participantsByMemberId.get(swap.memberIdTo());

            // Validate that both players belong to different teams
            if (p1.getTeamId() == null || p2.getTeamId() == null) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "cannot swap unassigned players (members with no team)");
            }

            if (p1.getTeamId().equals(p2.getTeamId())) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "cannot swap players from the same team");
            }

            // Validate that positions match (goalkeeper can only swap with goalkeeper)
            if (!p1.getPosition().equals(p2.getPosition())) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "can only swap players with the same position (LINE with LINE, GOAL with GOAL)");
            }

            // Perform the swap
            Long tempTeamId = p1.getTeamId();
            p1.assignTeam(p2.getTeamId());
            p2.assignTeam(tempTeamId);
        }

        // Save all updated participants
        matchParticipantRepository.saveAll(allParticipants);

        recalculateTeamScores(matchId);

        log.info(
                "player swaps completed matchId={} swapCount={}",
                matchId,
                swaps.size()
        );
    }

    public void delete(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "team not found"));
        Match match = requireMatch(team.getMatchId());
        clubAuthorizationService.requireDirector(match.getClubId());
        ensureMatchResultNotSet(match);
        teamRepository.delete(team);
    }

    private void recalculateTeamScores(UUID matchId) {
        List<MatchParticipant> participants = matchParticipantRepository.findByMatchId(matchId);
        List<Team> teams = teamRepository.findByMatchId(matchId);

        if (participants.isEmpty() || teams.isEmpty()) {
            return;
        }

        List<ClubMember> lineMembers = new ArrayList<>();
        List<ClubMember> goalMembers = new ArrayList<>();
        Map<Long, ClubMember> membersById = new HashMap<>();

        Match match = matchRepository.findById(matchId).orElseThrow();
        List<ClubMember> allMembers = clubMemberRepository.findByClubIdAndIdIn(
                match.getClubId(),
                participants.stream().map(MatchParticipant::getClubMemberId).distinct().toList()
        );
        for (ClubMember m : allMembers) {
            membersById.put(m.getId(), m);
        }

        for (MatchParticipant p : participants) {
            ClubMember m = membersById.get(p.getClubMemberId());
            if (m == null) continue;
            if (p.getPosition() == MatchParticipantPosition.LINE) {
                lineMembers.add(m);
            } else if (p.getPosition() == MatchParticipantPosition.GOAL) {
                goalMembers.add(m);
            }
        }

        List<ScoredMember> scoredLine = scoreMembers(lineMembers);
        List<ScoredMember> scoredGoal = scoreMembers(goalMembers);

        Map<Long, Double> scoreByMemberId = new HashMap<>();
        scoredLine.forEach(s -> scoreByMemberId.put(s.memberId(), s.score()));
        scoredGoal.forEach(s -> scoreByMemberId.put(s.memberId(), s.score()));

        Map<Long, List<MatchParticipant>> byTeam = participants.stream()
                .filter(p -> p.getTeamId() != null)
                .collect(Collectors.groupingBy(MatchParticipant::getTeamId));

        for (Team team : teams) {
            List<MatchParticipant> teamMembers = byTeam.getOrDefault(team.getId(), List.of());
            double totalScore = teamMembers.stream()
                    .mapToDouble(p -> scoreByMemberId.getOrDefault(p.getClubMemberId(), 0.0))
                    .sum();
            team.changeScore(totalScore);
        }
        teamRepository.saveAll(teams);
    }

    private static TeamResponseDTO toResponse(Team team) {
        return new TeamResponseDTO(team.getId(), team.getMatchId(), team.getClubJerseyId(), team.getScore());
    }

    private static List<Long> normalizeIds(List<Long> ids, String fieldName) {
        if (ids == null) {
            return List.of();
        }
        List<Long> normalized = ids.stream().filter(Objects::nonNull).toList();
        if (normalized.size() != ids.size()) {
            throw new ResponseStatusException(BAD_REQUEST, fieldName + " cannot contain nulls");
        }
        Set<Long> unique = new HashSet<>(normalized);
        if (unique.size() != normalized.size()) {
            throw new ResponseStatusException(BAD_REQUEST, fieldName + " cannot contain duplicates");
        }
        return normalized;
    }

    private Map<Long, ClubMember> loadMembersById(UUID clubId, List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ClubMember> found = clubMemberRepository.findByClubIdAndIdIn(clubId, ids);
        Map<Long, ClubMember> byId = new HashMap<>();
        for (ClubMember m : found) {
            byId.put(m.getId(), m);
        }

        List<Long> missing = ids.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "club members not found: " + missing);
        }
        return byId;
    }

    private Match requireMatch(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "match not found"));
    }

    private void validateJersey(UUID clubId, Long jerseyId) {
        if (jerseyId == null) {
            return;
        }
        ClubJersey jersey = clubJerseyRepository.findById(jerseyId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "jersey not found"));
        if (!clubId.equals(jersey.getClubId())) {
            throw new ResponseStatusException(BAD_REQUEST, "jersey not in club");
        }
    }

    private static void ensureMatchResultNotSet(Match match) {
        if (match.hasResult()) {
            throw new ResponseStatusException(BAD_REQUEST, "match result already set");
        }
    }

    private static List<Long> union(List<Long> a, List<Long> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        List<Long> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    private static int computeTeamCount(int lineCount, int maxLinePlayers) {
        if (lineCount < 2) {
            throw new ResponseStatusException(BAD_REQUEST, "need at least 2 line players");
        }
        // Align with the examples: 15 with max 5 => 3 teams; 16 with max 5 => 3 teams (6/5/5).
        int byRounding = Math.round((float) lineCount / (float) maxLinePlayers);
        return Math.max(2, Math.max(1, byRounding));
    }

    private static List<Integer> computeTeamSizes(int lineCount, int teamCount) {
        int base = lineCount / teamCount;
        int remainder = lineCount % teamCount;
        List<Integer> sizes = new ArrayList<>(teamCount);
        for (int i = 0; i < teamCount; i++) {
            sizes.add(base + (i < remainder ? 1 : 0));
        }
        return sizes;
    }

    private static List<ScoredMember> scoreMembers(List<ClubMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }

        int minRating = Integer.MAX_VALUE, maxRating = Integer.MIN_VALUE;
        int minChampion = Integer.MAX_VALUE, maxChampion = Integer.MIN_VALUE;
        int minMvp = Integer.MAX_VALUE, maxMvp = Integer.MIN_VALUE;

        for (ClubMember m : members) {
            int rating = safeInt(m.getRating());
            int champion = safeInt(m.getTimesChampion());
            int mvp = safeInt(m.getTimesMvp());

            minRating = Math.min(minRating, rating);
            maxRating = Math.max(maxRating, rating);
            minChampion = Math.min(minChampion, champion);
            maxChampion = Math.max(maxChampion, champion);
            minMvp = Math.min(minMvp, mvp);
            maxMvp = Math.max(maxMvp, mvp);
        }

        double ratingDen = (maxRating - minRating);
        double champDen = (maxChampion - minChampion);
        double mvpDen = (maxMvp - minMvp);

        List<ScoredMember> scored = new ArrayList<>(members.size());
        for (ClubMember m : members) {
            int rating = safeInt(m.getRating());
            int champion = safeInt(m.getTimesChampion());
            int mvp = safeInt(m.getTimesMvp());

            double normRating = ratingDen == 0 ? 0.0 : ((double) (rating - minRating) / ratingDen);
            double normChampion = champDen == 0 ? 0.0 : ((double) (champion - minChampion) / champDen);
            double normMvp = mvpDen == 0 ? 0.0 : ((double) (mvp - minMvp) / mvpDen);

            double score = normRating + normChampion + normMvp;
            scored.add(new ScoredMember(m.getId(), score));
        }
        return scored;
    }

    private static int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private static TeamBucket pickBucketForNextPlayer(List<TeamBucket> buckets) {
        return buckets.stream()
                .filter(b -> !b.isFull())
                .min(Comparator
                        .comparingDouble(TeamBucket::totalScore)
                        .thenComparingInt(TeamBucket::size)
                        .thenComparingLong(b -> b.teamId)
                )
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "not enough buckets to place members"));
    }

    private static void distributeLinePlayers(List<ScoredMember> scoredLineMembers, List<TeamBucket> buckets) {
        int n = buckets.size();
        for (int i = 0; i < scoredLineMembers.size(); i++) {
            int round = i / n;
            int slotInRound = i % n;
            // Even rounds: weakest-to-strongest (bucket n-1 -> 0).
            // Odd rounds: strongest-to-weakest (bucket 0 -> n-1).
            int bucketIndex = (round % 2 == 0)
                    ? n - 1 - slotInRound
                    : slotInRound;

            TeamBucket bucket = buckets.get(Math.min(bucketIndex, n - 1));
            // If the natural slot is full, pick the weakest available bucket.
            if (bucket.isFull()) {
                bucket = pickBucketForNextPlayer(buckets);
            }
            bucket.addLine(scoredLineMembers.get(i));
        }
    }

    private record ScoredMember(long memberId, double score) {
    }

    private static final class TeamBucket {
        private final long teamId;
        private final int targetSize;
        private final List<ScoredMember> lineMembers = new ArrayList<>();
        private double totalScore;
        private Long goalkeeperMemberId;

        private TeamBucket(long teamId, int targetSize) {
            this.teamId = teamId;
            this.targetSize = targetSize;
        }

        private boolean isFull() {
            return lineMembers.size() >= targetSize;
        }

        private int size() {
            return lineMembers.size();
        }

        private double totalScore() {
            return totalScore;
        }

        private void addLine(ScoredMember m) {
            if (isFull()) {
                throw new IllegalStateException("bucket is full");
            }
            lineMembers.add(m);
            totalScore += m.score();
        }

        private void assignGoalkeeper(ScoredMember keeper) {
            if (this.goalkeeperMemberId != null) {
                throw new IllegalStateException("goalkeeper already assigned");
            }
            this.goalkeeperMemberId = keeper.memberId();
            totalScore += keeper.score();
        }
    }
}
