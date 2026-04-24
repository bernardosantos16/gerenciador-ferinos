package com.bernardo.geradortimes.team.controller;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubJersey;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.match.model.Match;
import com.bernardo.geradortimes.match.model.MatchParticipant;
import com.bernardo.geradortimes.match.repository.MatchParticipantRepository;
import com.bernardo.geradortimes.match.repository.MatchRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.enums.MatchParticipantPosition;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.team.dto.request.CreateTeamRequestDTO;
import com.bernardo.geradortimes.team.dto.request.GenerateTeamsRequestDTO;
import com.bernardo.geradortimes.team.dto.request.PlayerSwapDTO;
import com.bernardo.geradortimes.team.dto.request.SwapPlayersRequestDTO;
import com.bernardo.geradortimes.team.dto.request.UpdateTeamJerseyRequestDTO;
import com.bernardo.geradortimes.team.model.Team;
import com.bernardo.geradortimes.team.repository.TeamRepository;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link TeamController}.
 * <p>
 * Covers: criar time, buscar por ID, listar, atualizar camisa, gerar times, trocar jogadores e deletar.
 */
@DisplayName("TeamController – Integration Tests")
class TeamControllerTest extends IntegrationTestBase {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Match persistMatch(UUID clubId) {
        return matchRepository.save(Match.create(clubId, Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    private Team persistTeam(UUID matchId, Long jerseyId) {
        return teamRepository.save(Team.create(matchId, jerseyId));
    }

    /**
     * Full setup: director + club + match + jersey.
     * Returns a context record for convenience.
     */
    private record TestContext(User director, Club club, Match match, ClubJersey jersey) {}

    private TestContext setupDirectorContext(String loginSuffix) {
        User director = createActiveUser("director_" + loginSuffix + "@team.com", "dir_" + loginSuffix);
        Club club = createClub("Clube " + loginSuffix, "clube_" + loginSuffix);
        createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
        Match match = persistMatch(club.getId());
        ClubJersey jersey = createJersey(club.getId(), "Camisa " + loginSuffix, "#FF0000");
        return new TestContext(director, club, match, jersey);
    }

    // ── POST /api/teams ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/teams")
    class CreateTeam {

        @Test
        @DisplayName("deve criar time e retornar 201 quando o usuário é DIRECTOR")
        void createSuccess() throws Exception {
            TestContext ctx = setupDirectorContext("create");

            var request = new CreateTeamRequestDTO(ctx.match().getId(), ctx.jersey().getId());

            mockMvc.perform(post("/api/teams")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.matchId", is(ctx.match().getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário é apenas MEMBER")
        void createForbiddenForMember() throws Exception {
            User member = createActiveUser("member_create@team.com", "mem_create");
            Club club = createClub("Clube Mem Create", "clube_mem_create");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());
            ClubJersey jersey = createJersey(club.getId(), "Camisa Mem", "#00FF00");

            var request = new CreateTeamRequestDTO(match.getId(), jersey.getId());

            mockMvc.perform(post("/api/teams")
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void createUnauthorized() throws Exception {
            var request = new CreateTeamRequestDTO(UUID.randomUUID(), 1L);

            mockMvc.perform(post("/api/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/teams/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/teams/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 quando o usuário é MEMBER do clube")
        void getByIdSuccess() throws Exception {
            User member = createActiveUser("member_get@team.com", "mem_get");
            Club club = createClub("Clube Get Team", "clube_get_team");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());
            Team team = persistTeam(match.getId(), null);

            mockMvc.perform(get("/api/teams/{id}", team.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(team.getId().intValue())));
        }

        @Test
        @DisplayName("deve retornar 404 quando o time não existe")
        void getByIdNotFound() throws Exception {
            TestContext ctx = setupDirectorContext("getnotfound");

            mockMvc.perform(get("/api/teams/{id}", 999999L)
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário não é membro do clube")
        void getByIdForbidden() throws Exception {
            User outsider = createActiveUser("outsider_get@team.com", "out_get");
            Club club = createClub("Clube Priv Team", "clube_priv_team");
            Match match = persistMatch(club.getId());
            Team team = persistTeam(match.getId(), null);

            mockMvc.perform(get("/api/teams/{id}", team.getId())
                            .header("Authorization", bearerToken(outsider)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/teams?matchId=... ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/teams?matchId=...")
    class ListTeams {

        @Test
        @DisplayName("deve retornar 200 com lista de times quando o usuário é MEMBER")
        void listSuccess() throws Exception {
            User member = createActiveUser("member_list@team.com", "mem_list");
            Club club = createClub("Clube List Team", "clube_list_team");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());
            persistTeam(match.getId(), null);
            persistTeam(match.getId(), null);

            mockMvc.perform(get("/api/teams")
                            .param("matchId", match.getId().toString())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário não é membro do clube")
        void listForbidden() throws Exception {
            User outsider = createActiveUser("outsider_list@team.com", "out_list");
            Club club = createClub("Clube Priv List", "clube_priv_list");
            Match match = persistMatch(club.getId());

            mockMvc.perform(get("/api/teams")
                            .param("matchId", match.getId().toString())
                            .header("Authorization", bearerToken(outsider)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PATCH /api/teams/{id}/jersey ──────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/teams/{id}/jersey")
    class UpdateJersey {

        @Test
        @DisplayName("deve atualizar a camisa do time e retornar 200")
        void updateJerseySuccess() throws Exception {
            TestContext ctx = setupDirectorContext("jersey");
            Team team = persistTeam(ctx.match().getId(), null);
            ClubJersey newJersey = createJersey(ctx.club().getId(), "Nova Camisa", "#0000FF");

            var request = new UpdateTeamJerseyRequestDTO(newJersey.getId());

            mockMvc.perform(patch("/api/teams/{id}/jersey", team.getId())
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clubJerseyId", is(newJersey.getId().intValue())));
        }

        @Test
        @DisplayName("deve retornar 404 quando o time não existe")
        void updateJerseyNotFound() throws Exception {
            TestContext ctx = setupDirectorContext("jersey_nf");

            var request = new UpdateTeamJerseyRequestDTO(ctx.jersey().getId());

            mockMvc.perform(patch("/api/teams/{id}/jersey", 999999L)
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/teams/generate ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/teams/generate")
    class GenerateTeams {

        @Test
        @DisplayName("deve gerar times balanceados e retornar 200")
        void generateSuccess() throws Exception {
            TestContext ctx = setupDirectorContext("gen");

            // Create 6 club members (line players)
            List<Long> lineIds = List.of(
                    createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId(),
                    createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId(),
                    createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId(),
                    createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId(),
                    createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId(),
                    createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId()
            );

            var request = new GenerateTeamsRequestDTO(
                    ctx.match().getId(),
                    lineIds,
                    List.of(),
                    3
            );

            mockMvc.perform(post("/api/teams/generate")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchId", is(ctx.match().getId().toString())))
                    .andExpect(jsonPath("$.teamCount", is(2)))
                    .andExpect(jsonPath("$.teams", hasSize(2)));
        }

        @Test
        @DisplayName("deve retornar 400 quando lineMemberIds tem menos de 2 membros")
        void generateTooFewPlayers() throws Exception {
            TestContext ctx = setupDirectorContext("gen_few");
            Long memberId = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId();

            var request = new GenerateTeamsRequestDTO(
                    ctx.match().getId(),
                    List.of(memberId),
                    List.of(),
                    3
            );

            mockMvc.perform(post("/api/teams/generate")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário é apenas MEMBER")
        void generateForbiddenForMember() throws Exception {
            User member = createActiveUser("member_gen@team.com", "mem_gen");
            Club club = createClub("Clube Gen Mem", "clube_gen_mem");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());

            var request = new GenerateTeamsRequestDTO(
                    match.getId(),
                    List.of(1L, 2L),
                    List.of(),
                    3
            );

            mockMvc.perform(post("/api/teams/generate")
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 400 quando um membro está em lineMemberIds e goalkeeperMemberIds")
        void generateOverlappingIds() throws Exception {
            TestContext ctx = setupDirectorContext("gen_overlap");
            Long memberId = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId();
            Long memberId2 = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER).getId();

            var request = new GenerateTeamsRequestDTO(
                    ctx.match().getId(),
                    List.of(memberId, memberId2),
                    List.of(memberId), // overlap!
                    3
            );

            mockMvc.perform(post("/api/teams/generate")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /api/teams/swap ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/teams/swap")
    class SwapPlayers {

        @Test
        @DisplayName("deve trocar jogadores entre times e retornar 204")
        void swapSuccess() throws Exception {
            TestContext ctx = setupDirectorContext("swap");

            // Create 2 members and 2 teams
            ClubMember m1 = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER);
            ClubMember m2 = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER);
            Team team1 = persistTeam(ctx.match().getId(), null);
            Team team2 = persistTeam(ctx.match().getId(), null);

            // Assign participants to different teams
            matchParticipantRepository.save(
                    MatchParticipant.create(ctx.match().getId(), m1.getId(), MatchParticipantPosition.LINE, team1.getId())
            );
            matchParticipantRepository.save(
                    MatchParticipant.create(ctx.match().getId(), m2.getId(), MatchParticipantPosition.LINE, team2.getId())
            );

            var request = new SwapPlayersRequestDTO(
                    ctx.match().getId(),
                    List.of(new PlayerSwapDTO(m1.getId(), m2.getId()))
            );

            mockMvc.perform(post("/api/teams/swap")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Verify the swap happened
            var p1 = matchParticipantRepository.findByMatchId(ctx.match().getId())
                    .stream().filter(p -> p.getClubMemberId().equals(m1.getId())).findFirst().orElseThrow();
            var p2 = matchParticipantRepository.findByMatchId(ctx.match().getId())
                    .stream().filter(p -> p.getClubMemberId().equals(m2.getId())).findFirst().orElseThrow();

            org.junit.jupiter.api.Assertions.assertEquals(team2.getId(), p1.getTeamId());
            org.junit.jupiter.api.Assertions.assertEquals(team1.getId(), p2.getTeamId());
        }

        @Test
        @DisplayName("deve retornar 400 quando os jogadores estão no mesmo time")
        void swapSameTeam() throws Exception {
            TestContext ctx = setupDirectorContext("swap_same");

            ClubMember m1 = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER);
            ClubMember m2 = createClubMember(null, ctx.club().getId(), ClubRole.MEMBER);
            Team team1 = persistTeam(ctx.match().getId(), null);

            matchParticipantRepository.save(
                    MatchParticipant.create(ctx.match().getId(), m1.getId(), MatchParticipantPosition.LINE, team1.getId())
            );
            matchParticipantRepository.save(
                    MatchParticipant.create(ctx.match().getId(), m2.getId(), MatchParticipantPosition.LINE, team1.getId())
            );

            var request = new SwapPlayersRequestDTO(
                    ctx.match().getId(),
                    List.of(new PlayerSwapDTO(m1.getId(), m2.getId()))
            );

            mockMvc.perform(post("/api/teams/swap")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando os times ainda não foram gerados")
        void swapNoTeams() throws Exception {
            TestContext ctx = setupDirectorContext("swap_noteams");

            var request = new SwapPlayersRequestDTO(
                    ctx.match().getId(),
                    List.of(new PlayerSwapDTO(1L, 2L))
            );

            mockMvc.perform(post("/api/teams/swap")
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário é apenas MEMBER")
        void swapForbiddenForMember() throws Exception {
            User member = createActiveUser("member_swap@team.com", "mem_swap");
            Club club = createClub("Clube Swap Mem", "clube_swap_mem");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());

            var request = new SwapPlayersRequestDTO(
                    match.getId(),
                    List.of(new PlayerSwapDTO(1L, 2L))
            );

            mockMvc.perform(post("/api/teams/swap")
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── DELETE /api/teams/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/teams/{id}")
    class DeleteTeam {

        @Test
        @DisplayName("deve retornar 204 quando o DIRECTOR deleta o time")
        void deleteSuccess() throws Exception {
            TestContext ctx = setupDirectorContext("delete");
            Team team = persistTeam(ctx.match().getId(), null);

            mockMvc.perform(delete("/api/teams/{id}", team.getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNoContent());

            org.junit.jupiter.api.Assertions.assertFalse(teamRepository.existsById(team.getId()));
        }

        @Test
        @DisplayName("deve retornar 404 quando o time não existe")
        void deleteNotFound() throws Exception {
            TestContext ctx = setupDirectorContext("delete_nf");

            mockMvc.perform(delete("/api/teams/{id}", 999999L)
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEMBER tenta deletar o time")
        void deleteForbiddenForMember() throws Exception {
            User member = createActiveUser("member_del@team.com", "mem_del");
            Club club = createClub("Clube Del Team", "clube_del_team");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());
            Team team = persistTeam(match.getId(), null);

            mockMvc.perform(delete("/api/teams/{id}", team.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }
    }
}

