package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
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
 * Integration tests for {@link ClubMemberController}.
 * <p>
 * Covers: Adição, busca, listagem paginada, atualização e remoção de membros de um clube.
 */
@DisplayName("ClubMemberController – Integration Tests")
class ClubMemberControllerTest extends IntegrationTestBase {

    private record TestContext(
            User director,
            User member,
            User outsider,
            Club club,
            ClubMember existingMember
    ) {}

    private TestContext setupContext(String suffix) {
        User director = createActiveUser("director_" + suffix + "@test.com", "director_" + suffix);
        User member = createActiveUser("member_" + suffix + "@test.com", "member_" + suffix);
        User outsider = createActiveUser("outsider_" + suffix + "@test.com", "outsider_" + suffix);

        Club club = createClub("Clube " + suffix, "clube_" + suffix);

        createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
        ClubMember existingMember = createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

        return new TestContext(director, member, outsider, club, existingMember);
    }

    // ── POST /api/clubs/{clubId}/members ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clubs/{clubId}/members")
    class AddClubMember {

        @Test
        @DisplayName("deve adicionar membro ao clube e retornar 201 quando usuário é DIRECTOR")
        void addSuccess() throws Exception {
            TestContext ctx = setupContext("add_success");

            Map<String, Object> request = Map.of(
                    "name", "Novo Jogador",
                    "position", "ATA"
            );

            mockMvc.perform(post("/api/clubs/{clubId}/members", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Novo Jogador")));
        }

        @Test
        @DisplayName("deve retornar 400 quando dados de adição são inválidos")
        void addBadRequest() throws Exception {
            TestContext ctx = setupContext("add_bad_req");

            // Requisição com dados faltando (assumindo que "name" é obrigatório)
            Map<String, Object> request = Map.of();

            mockMvc.perform(post("/api/clubs/{clubId}/members", ctx.club().getId())
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
                    "name", "Novo Jogador Sem Permissao"
            );

            mockMvc.perform(post("/api/clubs/{clubId}/members", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.member()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void addUnauthorized() throws Exception {
            Map<String, Object> request = Map.of("name", "Jogador Fantasma");

            mockMvc.perform(post("/api/clubs/{clubId}/members", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/clubs/{clubId}/members/{memberId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/{clubId}/members/{memberId}")
    class GetMemberById {

        @Test
        @DisplayName("deve buscar membro por ID e retornar 200")
        void getSuccess() throws Exception {
            TestContext ctx = setupContext("get_success");

            mockMvc.perform(get("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), ctx.existingMember().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ctx.existingMember().getId().intValue())));
        }

        @Test
        @DisplayName("deve retornar 404 quando membro não existe no clube informado")
        void getNotFound() throws Exception {
            TestContext ctx = setupContext("get_not_found");

            mockMvc.perform(get("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), 999999L)
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void getUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clubs/{clubId}/members/{memberId}", UUID.randomUUID(), 1L))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/clubs/{clubId}/members ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/{clubId}/members")
    class ListMembers {

        @Test
        @DisplayName("deve listar membros paginados e retornar 200 quando usuário pertence ao clube")
        void listSuccess() throws Exception {
            TestContext ctx = setupContext("list_success");

            mockMvc.perform(get("/api/clubs/{clubId}/members", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.member())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", not(empty())))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2))); // Diretor + Membro comum
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não pertence ao clube")
        void listForbiddenForOutsider() throws Exception {
            TestContext ctx = setupContext("list_forbidden");

            mockMvc.perform(get("/api/clubs/{clubId}/members", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.outsider())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void listUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clubs/{clubId}/members", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PATCH /api/clubs/{clubId}/members/{memberId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/clubs/{clubId}/members/{memberId}")
    class UpdateMember {

        @Test
        @DisplayName("deve atualizar membro e retornar 200 quando usuário é DIRECTOR")
        void updateSuccess() throws Exception {
            TestContext ctx = setupContext("patch_success");

            Map<String, Object> request = Map.of(
                    "nickname", "Apelido Atualizado"
            );

            mockMvc.perform(patch("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), ctx.existingMember().getId())
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não possui permissão de DIRECTOR")
        void updateForbiddenForMember() throws Exception {
            TestContext ctx = setupContext("patch_forbidden");

            Map<String, Object> request = Map.of(
                    "nickname", "Hack"
            );

            mockMvc.perform(patch("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), ctx.existingMember().getId())
                            .header("Authorization", bearerToken(ctx.member()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando membro a atualizar não existe")
        void updateNotFound() throws Exception {
            TestContext ctx = setupContext("patch_not_found");

            Map<String, Object> request = Map.of(
                    "nickname", "Inexistente"
            );

            mockMvc.perform(patch("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), 999999L)
                            .header("Authorization", bearerToken(ctx.director()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void updateUnauthorized() throws Exception {
            Map<String, Object> request = Map.of("nickname", "Sem Login");

            mockMvc.perform(patch("/api/clubs/{clubId}/members/{memberId}", UUID.randomUUID(), 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── DELETE /api/clubs/{clubId}/members/{memberId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/clubs/{clubId}/members/{memberId}")
    class DeleteMember {

        @Test
        @DisplayName("deve remover membro e retornar 204 quando usuário é DIRECTOR")
        void deleteSuccess() throws Exception {
            TestContext ctx = setupContext("delete_success");

            mockMvc.perform(delete("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), ctx.existingMember().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNoContent());

            // Validação de que realmente foi removido através do endpoint de busca
            mockMvc.perform(get("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), ctx.existingMember().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não possui permissão de DIRECTOR")
        void deleteForbiddenForMember() throws Exception {
            TestContext ctx = setupContext("delete_forbidden");

            mockMvc.perform(delete("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), ctx.existingMember().getId())
                            .header("Authorization", bearerToken(ctx.member())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando membro a remover não existe")
        void deleteNotFound() throws Exception {
            TestContext ctx = setupContext("delete_not_found");

            mockMvc.perform(delete("/api/clubs/{clubId}/members/{memberId}", ctx.club().getId(), 999999L)
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void deleteUnauthorized() throws Exception {
            mockMvc.perform(delete("/api/clubs/{clubId}/members/{memberId}", UUID.randomUUID(), 1L))
                    .andExpect(status().isUnauthorized());
        }
    }
}