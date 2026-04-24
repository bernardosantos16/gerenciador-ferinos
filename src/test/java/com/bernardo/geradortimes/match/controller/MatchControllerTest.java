package com.bernardo.geradortimes.match.controller;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.match.dto.request.CreateMatchRequestDTO;
import com.bernardo.geradortimes.match.model.Match;
import com.bernardo.geradortimes.match.model.MatchParticipant;
import com.bernardo.geradortimes.match.repository.MatchParticipantRepository;
import com.bernardo.geradortimes.match.repository.MatchRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.enums.MatchParticipantPosition;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Match persistMatch(UUID clubId) {
        return matchRepository.save(Match.create(clubId, Instant.now().plus(1, ChronoUnit.DAYS)));
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
}

