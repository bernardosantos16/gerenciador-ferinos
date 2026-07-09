package com.bernardo.geradortimes.match.controller;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.match.dto.request.CreateMatchBatchRequestDTO;
import com.bernardo.geradortimes.match.dto.request.CreateMatchRequestDTO;
import com.bernardo.geradortimes.match.dto.request.SetMatchResultRequestDTO;
import com.bernardo.geradortimes.match.model.Match;
import com.bernardo.geradortimes.match.model.MatchParticipant;
import com.bernardo.geradortimes.match.repository.MatchParticipantRepository;
import com.bernardo.geradortimes.match.repository.MatchRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.enums.MatchParticipantPosition;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.team.model.Team;
import com.bernardo.geradortimes.team.repository.TeamRepository;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link MatchController}.
 * <p>
 * Covers: criar partida, buscar por ID, listar por clube, listar participantes e deletar.
 */
@DisplayName("MatchController – Integration Tests")
class MatchControllerTest extends IntegrationTestBase {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    @Autowired
    private TeamRepository teamRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Match persistMatch(UUID clubId) {
        return matchRepository.save(Match.create(clubId, Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    private Match persistPastMatch(UUID clubId) {
        return matchRepository.save(Match.create(clubId, Instant.now().minus(1, ChronoUnit.DAYS)));
    }

    private Team persistTeam(UUID matchId) {
        return teamRepository.save(Team.create(matchId, null));
    }

    // ── POST /api/matches ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/matches")
    class CreateMatch {

        @Test
        @DisplayName("deve criar partida e retornar 201 quando o usuário é DIRECTOR do clube")
        void createSuccess() throws Exception {
            User director = createActiveUser("director@match.com", "director_match");
            Club club = createClub("Clube Teste", "clube_teste");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new CreateMatchRequestDTO(
                    club.getId(),
                    Instant.now().plus(2, ChronoUnit.DAYS)
            );

            mockMvc.perform(post("/api/matches")
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())))
                    .andExpect(jsonPath("$.clubId", is(club.getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário é apenas MEMBER (não DIRECTOR)")
        void createForbiddenForMember() throws Exception {
            User member = createActiveUser("member@match.com", "member_match");
            Club club = createClub("Clube Membro", "clube_membro");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            var request = new CreateMatchRequestDTO(
                    club.getId(),
                    Instant.now().plus(2, ChronoUnit.DAYS)
            );

            mockMvc.perform(post("/api/matches")
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário não pertence ao clube")
        void createForbiddenForNonMember() throws Exception {
            User outsider = createActiveUser("outsider@match.com", "outsider_match");
            Club club = createClub("Clube Externo", "clube_externo");

            var request = new CreateMatchRequestDTO(
                    club.getId(),
                    Instant.now().plus(2, ChronoUnit.DAYS)
            );

            mockMvc.perform(post("/api/matches")
                            .header("Authorization", bearerToken(outsider))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void createUnauthorized() throws Exception {
            var request = new CreateMatchRequestDTO(
                    UUID.randomUUID(),
                    Instant.now().plus(2, ChronoUnit.DAYS)
            );

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando dateTime está no passado")
        void createPastDateTime() throws Exception {
            User director = createActiveUser("director2@match.com", "director2_match");
            Club club = createClub("Clube Past", "clube_past");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new CreateMatchRequestDTO(
                    club.getId(),
                    Instant.now().minus(1, ChronoUnit.DAYS)
            );

            mockMvc.perform(post("/api/matches")
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/matches/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/matches/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 quando o usuário é MEMBER do clube da partida")
        void getByIdSuccess() throws Exception {
            User member = createActiveUser("member2@match.com", "member2_match");
            Club club = createClub("Clube Get", "clube_get");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());

            mockMvc.perform(get("/api/matches/{id}", match.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(match.getId().toString())))
                    .andExpect(jsonPath("$.clubId", is(club.getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário não é membro do clube")
        void getByIdForbidden() throws Exception {
            User outsider = createActiveUser("outsider2@match.com", "outsider2_match");
            Club club = createClub("Clube Privado", "clube_privado");
            Match match = persistMatch(club.getId());

            mockMvc.perform(get("/api/matches/{id}", match.getId())
                            .header("Authorization", bearerToken(outsider)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando a partida não existe")
        void getByIdNotFound() throws Exception {
            User director = createActiveUser("director3@match.com", "director3_match");
            Club club = createClub("Clube 404", "clube_404");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            mockMvc.perform(get("/api/matches/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(director)))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/matches?clubId=... ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/matches?clubId=...")
    class ListByClub {

        @Test
        @DisplayName("deve retornar 200 com lista de partidas quando o usuário é MEMBER")
        void listSuccess() throws Exception {
            User member = createActiveUser("member3@match.com", "member3_match");
            Club club = createClub("Clube List", "clube_list");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            persistMatch(club.getId());
            persistMatch(club.getId());

            mockMvc.perform(get("/api/matches")
                            .param("clubId", club.getId().toString())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("deve retornar 403 quando o usuário não é membro do clube")
        void listForbidden() throws Exception {
            User outsider = createActiveUser("outsider3@match.com", "outsider3_match");
            Club club = createClub("Clube Restrito", "clube_restrito");

            mockMvc.perform(get("/api/matches")
                            .param("clubId", club.getId().toString())
                            .header("Authorization", bearerToken(outsider)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/matches/{id}/participants ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/matches/{id}/participants")
    class ListParticipants {

        @Test
        @DisplayName("deve retornar 200 com lista de participantes quando o usuário é MEMBER")
        void listParticipantsSuccess() throws Exception {
            User member = createActiveUser("member4@match.com", "member4_match");
            Club club = createClub("Clube Part", "clube_part");
            ClubMember clubMember = createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());

            // Add a participant
            matchParticipantRepository.save(
                    MatchParticipant.create(match.getId(), clubMember.getId(), MatchParticipantPosition.LINE, null)
            );

            mockMvc.perform(get("/api/matches/{id}/participants", match.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].clubMemberId", is(clubMember.getId().intValue())));
        }

        @Test
        @DisplayName("deve retornar 404 quando a partida não existe")
        void listParticipantsNotFound() throws Exception {
            User member = createActiveUser("member5@match.com", "member5_match");
            Club club = createClub("Clube Part2", "clube_part2");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            mockMvc.perform(get("/api/matches/{id}/participants", UUID.randomUUID())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/matches/{id}/result ───────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/matches/{id}/result")
    class SetResult {

        @Test
        @DisplayName("deve definir campeao e MVP, incrementando estatisticas dos membros corretos")
        void setResultSuccess() throws Exception {
            User director = createActiveUser("director_result@match.com", "director_result");
            Club club = createClub("Clube Resultado", "clube_resultado");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember player1 = createClubMember(null, club.getId(), ClubRole.MEMBER);
            ClubMember player2 = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team championTeam = persistTeam(match.getId());

            matchParticipantRepository.save(MatchParticipant.create(
                    match.getId(),
                    player1.getId(),
                    MatchParticipantPosition.LINE,
                    championTeam.getId()
            ));
            matchParticipantRepository.save(MatchParticipant.create(
                    match.getId(),
                    player2.getId(),
                    MatchParticipantPosition.LINE,
                    championTeam.getId()
            ));

            var request = new SetMatchResultRequestDTO(championTeam.getId(), player2.getId());

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.teamChampionId", is(championTeam.getId().intValue())))
                    .andExpect(jsonPath("$.clubMemberMvpId", is(player2.getId().intValue())));

            ClubMember updatedPlayer1 = clubMemberRepository.findById(player1.getId()).orElseThrow();
            ClubMember updatedPlayer2 = clubMemberRepository.findById(player2.getId()).orElseThrow();
            assertEquals(1, updatedPlayer1.getTimesChampion());
            assertEquals(0, updatedPlayer1.getTimesMvp());
            assertEquals(1, updatedPlayer2.getTimesChampion());
            assertEquals(1, updatedPlayer2.getTimesMvp());
        }

        @Test
        @DisplayName("deve retornar 400 quando tentam definir o resultado novamente")
        void setResultAlreadySet() throws Exception {
            User director = createActiveUser("director_idempotent@match.com", "director_idempotent");
            Club club = createClub("Clube Idempotente", "clube_idempotente");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember player = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team championTeam = persistTeam(match.getId());
            matchParticipantRepository.save(MatchParticipant.create(
                    match.getId(),
                    player.getId(),
                    MatchParticipantPosition.LINE,
                    championTeam.getId()
            ));

            var request = new SetMatchResultRequestDTO(championTeam.getId(), player.getId());

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk());
            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());

            ClubMember updatedPlayer = clubMemberRepository.findById(player.getId()).orElseThrow();
            assertEquals(1, updatedPlayer.getTimesChampion());
            assertEquals(1, updatedPlayer.getTimesMvp());
        }

        @Test
        @DisplayName("deve retornar 400 ao tentar trocar campeao e MVP ja definidos")
        void setResultChangeBlocked() throws Exception {
            User director = createActiveUser("director_change_result@match.com", "director_change_result");
            Club club = createClub("Clube Troca Resultado", "clube_troca_resultado");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember player1 = createClubMember(null, club.getId(), ClubRole.MEMBER);
            ClubMember player2 = createClubMember(null, club.getId(), ClubRole.MEMBER);
            ClubMember player3 = createClubMember(null, club.getId(), ClubRole.MEMBER);
            ClubMember player4 = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team team1 = persistTeam(match.getId());
            Team team2 = persistTeam(match.getId());

            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player1.getId(), MatchParticipantPosition.LINE, team1.getId()));
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player2.getId(), MatchParticipantPosition.LINE, team1.getId()));
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player3.getId(), MatchParticipantPosition.LINE, team2.getId()));
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player4.getId(), MatchParticipantPosition.LINE, team2.getId()));

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new SetMatchResultRequestDTO(team1.getId(), player2.getId()))))
                    .andExpect(status().isOk());
            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new SetMatchResultRequestDTO(team2.getId(), player3.getId()))))
                    .andExpect(status().isBadRequest());

            ClubMember updatedPlayer1 = clubMemberRepository.findById(player1.getId()).orElseThrow();
            ClubMember updatedPlayer2 = clubMemberRepository.findById(player2.getId()).orElseThrow();
            ClubMember updatedPlayer3 = clubMemberRepository.findById(player3.getId()).orElseThrow();
            ClubMember updatedPlayer4 = clubMemberRepository.findById(player4.getId()).orElseThrow();
            assertEquals(1, updatedPlayer1.getTimesChampion());
            assertEquals(1, updatedPlayer2.getTimesChampion());
            assertEquals(1, updatedPlayer2.getTimesMvp());
            assertEquals(0, updatedPlayer3.getTimesChampion());
            assertEquals(0, updatedPlayer3.getTimesMvp());
            assertEquals(0, updatedPlayer4.getTimesChampion());
        }

        @Test
        @DisplayName("deve retornar 400 quando a partida ainda não foi realizada")
        void setResultFutureMatch() throws Exception {
            User director = createActiveUser("director_future_result@match.com", "director_future_result");
            Club club = createClub("Clube Resultado Futuro", "clube_resultado_futuro");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember player = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());
            Team team = persistTeam(match.getId());
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player.getId(), MatchParticipantPosition.LINE, team.getId()));

            var request = new SetMatchResultRequestDTO(team.getId(), player.getId());

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando o MVP não está na partida")
        void setResultMvpNotParticipant() throws Exception {
            User director = createActiveUser("director_mvp_out@match.com", "director_mvp_out");
            Club club = createClub("Clube MVP Fora", "clube_mvp_fora");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember player = createClubMember(null, club.getId(), ClubRole.MEMBER);
            ClubMember notParticipant = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team team = persistTeam(match.getId());
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player.getId(), MatchParticipantPosition.LINE, team.getId()));

            var request = new SetMatchResultRequestDTO(team.getId(), notParticipant.getId());

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando participante injetado não pertence ao clube da partida")
        void setResultRejectsParticipantOutsideMatchClub() throws Exception {
            User director = createActiveUser("director_bad_participant@match.com", "director_bad_participant");
            Club club = createClub("Clube Participante Valido", "clube_participante_valido");
            Club otherClub = createClub("Clube Participante Invalido", "clube_participante_invalido");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember validPlayer = createClubMember(null, club.getId(), ClubRole.MEMBER);
            ClubMember outsidePlayer = createClubMember(null, otherClub.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team team = persistTeam(match.getId());
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), validPlayer.getId(), MatchParticipantPosition.LINE, team.getId()));
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), outsidePlayer.getId(), MatchParticipantPosition.LINE, team.getId()));

            var request = new SetMatchResultRequestDTO(team.getId(), validPlayer.getId());

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEMBER tenta definir resultado")
        void setResultForbiddenForMember() throws Exception {
            User member = createActiveUser("member_result@match.com", "member_result");
            Club club = createClub("Clube Resultado Member", "clube_resultado_member");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            ClubMember player = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team team = persistTeam(match.getId());
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player.getId(), MatchParticipantPosition.LINE, team.getId()));

            var request = new SetMatchResultRequestDTO(team.getId(), player.getId());

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── DELETE /api/matches/{id} ──────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/matches/{id}")
    class DeleteMatch {

        @Test
        @DisplayName("deve retornar 204 quando o DIRECTOR deleta a partida")
        void deleteSuccess() throws Exception {
            User director = createActiveUser("director4@match.com", "director4_match");
            Club club = createClub("Clube Del", "clube_del");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            Match match = persistMatch(club.getId());

            mockMvc.perform(delete("/api/matches/{id}", match.getId())
                            .header("Authorization", bearerToken(director)))
                    .andExpect(status().isNoContent());

            org.junit.jupiter.api.Assertions.assertFalse(matchRepository.existsById(match.getId()));
        }

        @Test
        @DisplayName("deve retornar 400 quando tentam deletar partida com resultado definido")
        void deleteBlockedAfterResult() throws Exception {
            User director = createActiveUser("director_delete_result@match.com", "director_delete_result");
            Club club = createClub("Clube Delete Resultado", "clube_delete_resultado");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            ClubMember player = createClubMember(null, club.getId(), ClubRole.MEMBER);
            Match match = persistPastMatch(club.getId());
            Team team = persistTeam(match.getId());
            matchParticipantRepository.save(MatchParticipant.create(match.getId(), player.getId(), MatchParticipantPosition.LINE, team.getId()));

            mockMvc.perform(patch("/api/matches/{id}/result", match.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new SetMatchResultRequestDTO(team.getId(), player.getId()))))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/matches/{id}", match.getId())
                            .header("Authorization", bearerToken(director)))
                    .andExpect(status().isBadRequest());

            org.junit.jupiter.api.Assertions.assertTrue(matchRepository.existsById(match.getId()));
        }

        @Test
        @DisplayName("deve retornar 403 quando MEMBER tenta deletar a partida")
        void deleteForbiddenForMember() throws Exception {
            User director = createActiveUser("director5@match.com", "director5_match");
            User member = createActiveUser("member6@match.com", "member6_match");
            Club club = createClub("Clube Del2", "clube_del2");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);
            Match match = persistMatch(club.getId());

            mockMvc.perform(delete("/api/matches/{id}", match.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando a partida não existe")
        void deleteNotFound() throws Exception {
            User director = createActiveUser("director6@match.com", "director6_match");
            Club club = createClub("Clube Del3", "clube_del3");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            mockMvc.perform(delete("/api/matches/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(director)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/matches/batch")
    class CreateBatchMatch {

        @Test
        @DisplayName("deve criar partidas recorrentes e retornar 201")
        void createBatchSuccess() throws Exception {
            User director = createActiveUser("director_batch@match.com", "director_batch");
            Club club = createClub("Clube Batch", "clube_batch");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate startDate = today.plusDays(7);
            LocalDate endDate = today.plusDays(35);
            DayOfWeek dayOfWeek = startDate.getDayOfWeek();

            var request = new CreateMatchBatchRequestDTO(
                    club.getId(), dayOfWeek, LocalTime.of(20, 0), startDate, endDate, ZoneOffset.UTC
            );

            mockMvc.perform(post("/api/matches/batch")
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].clubId", is(club.getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuario nao e DIRECTOR")
        void createBatchForbiddenForMember() throws Exception {
            User member = createActiveUser("member_batch@match.com", "member_batch");
            Club club = createClub("Clube Batch2", "clube_batch2");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate startDate = today.plusDays(7);
            LocalDate endDate = today.plusDays(14);
            DayOfWeek dayOfWeek = startDate.getDayOfWeek();

            var request = new CreateMatchBatchRequestDTO(
                    club.getId(), dayOfWeek, LocalTime.of(20, 0), startDate, endDate, ZoneOffset.UTC
            );

            mockMvc.perform(post("/api/matches/batch")
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando nao autenticado")
        void createBatchUnauthorized() throws Exception {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            var request = new CreateMatchBatchRequestDTO(
                    UUID.randomUUID(),
                    DayOfWeek.TUESDAY,
                    LocalTime.of(20, 0),
                    today.plusDays(7),
                    today.plusDays(14),
                    ZoneOffset.UTC
            );

            mockMvc.perform(post("/api/matches/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando endDate e anterior a startDate")
        void createBatchEndBeforeStart() throws Exception {
            User director = createActiveUser("director_batch2@match.com", "director_batch2");
            Club club = createClub("Clube Batch3", "clube_batch3");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate startDate = today.plusDays(7);

            var request = new CreateMatchBatchRequestDTO(
                    club.getId(),
                    DayOfWeek.TUESDAY,
                    LocalTime.of(20, 0),
                    startDate,
                    startDate.minusDays(1),
                    ZoneOffset.UTC
            );

            mockMvc.perform(post("/api/matches/batch")
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nenhuma data no intervalo corresponde ao dia da semana")
        void createBatchNoDatesFound() throws Exception {
            User director = createActiveUser("director_batch3@match.com", "director_batch3");
            Club club = createClub("Clube Batch4", "clube_batch4");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate startDate = today.plusDays(7);
            LocalDate endDate = today.plusDays(8);
            DayOfWeek differentDay = startDate.getDayOfWeek().plus(2);

            var request = new CreateMatchBatchRequestDTO(
                    club.getId(), differentDay, LocalTime.of(20, 0), startDate, endDate, ZoneOffset.UTC
            );

            mockMvc.perform(post("/api/matches/batch")
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando startDate esta no passado")
        void createBatchStartInPast() throws Exception {
            User director = createActiveUser("director_batch4@match.com", "director_batch4");
            Club club = createClub("Clube Batch5", "clube_batch5");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            LocalDate today = LocalDate.now(ZoneOffset.UTC);

            var request = new CreateMatchBatchRequestDTO(
                    club.getId(),
                    DayOfWeek.TUESDAY,
                    LocalTime.of(20, 0),
                    today.minusDays(1),
                    today.plusDays(14),
                    ZoneOffset.UTC
            );

            mockMvc.perform(post("/api/matches/batch")
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
