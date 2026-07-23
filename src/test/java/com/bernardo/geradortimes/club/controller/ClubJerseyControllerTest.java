package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubJersey;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ClubJerseyController}.
 * <p>
 * Covers: Criação, listagem, atualização e remoção de camisas (uniformes) de um clube.
 */
@DisplayName("ClubJerseyController – Integration Tests")
class ClubJerseyControllerTest extends IntegrationTestBase {

    private record TestContext(
            User director,
            User member,
            User outsider,
            Club club,
            ClubJersey jersey
    ) {}

    private TestContext setupContext(String suffix) {
        User director = createActiveUser("director_" + suffix + "@test.com", "director_" + suffix);
        User member = createActiveUser("member_" + suffix + "@test.com", "member_" + suffix);
        User outsider = createActiveUser("outsider_" + suffix + "@test.com", "outsider_" + suffix);

        Club club = createClub("Clube " + suffix, "clube_" + suffix);

        createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
        createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

        ClubJersey jersey = createJersey(club.getId(), "Principal", "#0000ff");

        return new TestContext(director, member, outsider, club, jersey);
    }

    // ── POST /api/clubs/{clubId}/jerseys ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clubs/{clubId}/jerseys")
    class AddJersey {

        @Test
        @DisplayName("deve adicionar camisa ao clube e retornar 201 quando usuário é DIRECTOR")
        void addSuccess() throws Exception {
            TestContext ctx = setupContext("add_success");

            Map<String, Object> request = Map.of(
                    "name", "Branco",
                    "hexColor", "#ffffff",
                    "isGoalkeeperJersey", false
            );

            mockMvc.perform(post("/api/clubs/{clubId}/jerseys", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Branco")));
        }

        @Test
        @DisplayName("deve retornar 400 quando dados de adição da camisa são inválidos")
        void addBadRequest() throws Exception {
            TestContext ctx = setupContext("add_bad_req");

            // Requisição vazia para falhar na validação
            Map<String, Object> request = Map.of(
                    "name", "Principal",
                    "hexColor", "Preto",
                    "isGoalkeeperJersey", false
            );

            mockMvc.perform(post("/api/clubs/{clubId}/jerseys", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário logado não possui permissão de DIRECTOR")
        void addForbiddenForMember() throws Exception {
            TestContext ctx = setupContext("add_forbidden");

            Map<String, Object> request = Map.of(
                    "name", "Branco",
                    "hexColor", "#ffffff",
                    "isGoalkeeperJersey", false
            );

            mockMvc.perform(post("/api/clubs/{clubId}/jerseys", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.member()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void addUnauthorized() throws Exception {
            Map<String, Object> request = Map.of("name", "Fantasma", "color", "Invisível");

            mockMvc.perform(post("/api/clubs/{clubId}/jerseys", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/clubs/{clubId}/jerseys ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/{clubId}/jerseys")
    class ListJerseys {

        @Test
        @DisplayName("deve listar camisas e retornar 200 quando usuário pertence ao clube")
        void listSuccess() throws Exception {
            TestContext ctx = setupContext("list_success");

            mockMvc.perform(get("/api/clubs/{clubId}/jerseys", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.member())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.content[0].id", is(ctx.jersey().getId().intValue())));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não pertence ao clube")
        void listForbiddenForOutsider() throws Exception {
            TestContext ctx = setupContext("list_forbidden");

            mockMvc.perform(get("/api/clubs/{clubId}/jerseys", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.outsider())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void listUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clubs/{clubId}/jerseys", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PATCH /api/clubs/{clubId}/jerseys/{jerseyId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/clubs/{clubId}/jerseys/{jerseyId}")
    class UpdateJersey {

        @Test
        @DisplayName("deve atualizar camisa e retornar 200 quando usuário é DIRECTOR")
        void updateSuccess() throws Exception {
            TestContext ctx = setupContext("patch_success");

            Map<String, Object> request = Map.of(
                    "name", "Nome Atualizado"
            );

            mockMvc.perform(patch("/api/clubs/{clubId}/jerseys/{jerseyId}", ctx.club().getId(), ctx.jersey().getId())
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Nome Atualizado")));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não possui permissão de DIRECTOR")
        void updateForbiddenForMember() throws Exception {
            TestContext ctx = setupContext("patch_forbidden");

            Map<String, Object> request = Map.of(
                    "name", "Tentativa Falha"
            );

            mockMvc.perform(patch("/api/clubs/{clubId}/jerseys/{jerseyId}", ctx.club().getId(), ctx.jersey().getId())
                            .header("Authorization", bearerToken(ctx.member()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando camisa a atualizar não existe")
        void updateNotFound() throws Exception {
            TestContext ctx = setupContext("patch_not_found");

            Map<String, Object> request = Map.of(
                    "name", "Inexistente"
            );

            mockMvc.perform(patch("/api/clubs/{clubId}/jerseys/{jerseyId}", ctx.club().getId(), 999999L)
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void updateUnauthorized() throws Exception {
            Map<String, Object> request = Map.of("name", "Sem Login");

            mockMvc.perform(patch("/api/clubs/{clubId}/jerseys/{jerseyId}", UUID.randomUUID(), 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── DELETE /api/clubs/{clubId}/jerseys/{jerseyId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/clubs/{clubId}/jerseys/{jerseyId}")
    class DeleteJersey {

        @Test
        @DisplayName("deve remover camisa e retornar 204 quando usuário é DIRECTOR")
        void deleteSuccess() throws Exception {
            TestContext ctx = setupContext("delete_success");

            mockMvc.perform(delete("/api/clubs/{clubId}/jerseys/{jerseyId}", ctx.club().getId(), ctx.jersey().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNoContent());

            // Validação de que realmente foi removido listando as camisas (deve vir vazio ou não conter a removida)
            mockMvc.perform(get("/api/clubs/{clubId}/jerseys", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não possui permissão de DIRECTOR")
        void deleteForbiddenForMember() throws Exception {
            TestContext ctx = setupContext("delete_forbidden");

            mockMvc.perform(delete("/api/clubs/{clubId}/jerseys/{jerseyId}", ctx.club().getId(), ctx.jersey().getId())
                            .header("Authorization", bearerToken(ctx.member())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando camisa a remover não existe")
        void deleteNotFound() throws Exception {
            TestContext ctx = setupContext("delete_not_found");

            mockMvc.perform(delete("/api/clubs/{clubId}/jerseys/{jerseyId}", ctx.club().getId(), 999999L)
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void deleteUnauthorized() throws Exception {
            mockMvc.perform(delete("/api/clubs/{clubId}/jerseys/{jerseyId}", UUID.randomUUID(), 1L))
                    .andExpect(status().isUnauthorized());
        }
    }
}