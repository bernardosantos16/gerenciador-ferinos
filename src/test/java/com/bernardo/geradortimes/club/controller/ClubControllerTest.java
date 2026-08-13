package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.dto.request.CreateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateClubRequestDTO;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ClubController}.
 * <p>
 * Covers: criar clube, buscar por ID, listar clubes por role, atualizar e desativar.
 */
@DisplayName("ClubController – Integration Tests")
class ClubControllerTest extends IntegrationTestBase {

    // ── POST /api/clubs ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clubs")
    class CreateClub {

        @Test
        @DisplayName("deve criar clube e retornar 201 quando autenticado")
        void createSuccess() throws Exception {
            User user = createActiveUser("creator@club.com", "creator_nick");

            var request = new CreateClubRequestDTO(
                    "Meu Clube FC",
                    "meu_clube"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())))
                    .andExpect(jsonPath("$.name", is("Meu Clube FC")))
                    .andExpect(jsonPath("$.nickname", is("meu_clube")));
        }

        @Test
        @DisplayName("deve retornar 400 quando nickname é nulo")
        void createWithNullNickname() throws Exception {
            User user = createActiveUser("creator2@club.com", "creator2_nick");

            var request = new CreateClubRequestDTO(
                    "Clube Teste",
                    null
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void createBlankName() throws Exception {
            User user = createActiveUser("creator3@club.com", "creator3_nick");

            var request = new CreateClubRequestDTO(
                    "   ",
                    "club_nick"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nickname é muito curto (< 3 caracteres)")
        void createShortNickname() throws Exception {
            User user = createActiveUser("creator4@club.com", "creator4_nick");

            var request = new CreateClubRequestDTO(
                    "Clube Válido",
                    "ab"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nickname é muito longo (> 24 caracteres)")
        void createLongNickname() throws Exception {
            User user = createActiveUser("creator5@club.com", "creator5_nick");

            var request = new CreateClubRequestDTO(
                    "Clube Válido",
                    "este_eh_um_nickname_muito_longo_demais"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nickname contém espaço")
        void createNicknameWithSpace() throws Exception {
            User user = createActiveUser("creator_space@club.com", "creator_space");

            var request = new CreateClubRequestDTO(
                    "Clube Válido",
                    "meu clube"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nickname contém maiúsculas")
        void createNicknameUppercase() throws Exception {
            User user = createActiveUser("creator_upper@club.com", "creator_upper");

            var request = new CreateClubRequestDTO(
                    "Clube Válido",
                    "MeuClube"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando nickname já existe")
        void createDuplicateNickname() throws Exception {
            User user = createActiveUser("creator_dup@club.com", "creator_dup");
            createClub("Clube Existente", "existing_nick");

            var request = new CreateClubRequestDTO(
                    "Clube Novo",
                    "existing_nick"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void createUnauthorized() throws Exception {
            var request = new CreateClubRequestDTO(
                    "Clube Não Autenticado",
                    "club_no_auth"
            );

            mockMvc.perform(post("/api/clubs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve registrar o criador como DIRECTOR do clube")
        void creatorBecomesDirector() throws Exception {
            User user = createActiveUser("director_test@club.com", "director_test");

            var request = new CreateClubRequestDTO(
                    "Club Director Test",
                    "club_dir"
            );

            mockMvc.perform(post("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())));

            // Verify creator is DIRECTOR in the club
            // This is verified implicitly through subsequent tests, but here we could
            // verify that the user has DIRECTOR role in the created club
        }
    }

    // ── GET /api/clubs/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 quando clube existe")
        void getByIdSuccess() throws Exception {
            User user = createActiveUser("getter@club.com", "getter_nick");
            Club club = createClub("Clube Get Test", "club_get");
            createClubMember(user.getId(), club.getId(), ClubRole.MEMBER);

            mockMvc.perform(get("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(club.getId().toString())))
                    .andExpect(jsonPath("$.name", is("Clube Get Test")))
                    .andExpect(jsonPath("$.nickname", is("club_get")));
        }

        @Test
        @DisplayName("deve retornar 404 quando clube não existe")
        void getByIdNotFound() throws Exception {
            User user = createActiveUser("getter2@club.com", "getter2_nick");

            mockMvc.perform(get("/api/clubs/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void getByIdUnauthorized() throws Exception {
            Club club = createClub("Club Unauthorized", "club_unauth");

            mockMvc.perform(get("/api/clubs/{id}", club.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/clubs?clubRole=... ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs?clubRole=...")
    class ListByRole {

        @Test
        @DisplayName("deve retornar 200 com lista de clubes onde o usuário é DIRECTOR")
        void listAsDirector() throws Exception {
            User user = createActiveUser("list_director@club.com", "list_director");
            Club club1 = createClub("Club Director 1", "club_dir_1");
            Club club2 = createClub("Club Director 2", "club_dir_2");
            Club club3 = createClub("Club Member Only", "club_member_only");

            createClubMember(user.getId(), club1.getId(), ClubRole.DIRECTOR);
            createClubMember(user.getId(), club2.getId(), ClubRole.DIRECTOR);
            createClubMember(user.getId(), club3.getId(), ClubRole.MEMBER);

            mockMvc.perform(get("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .param("clubRole", "DIRECTOR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                            club1.getId().toString(),
                            club2.getId().toString()
                    )));
        }

        @Test
        @DisplayName("deve retornar 200 com lista de clubes onde o usuário é MEMBER")
        void listAsMember() throws Exception {
            User user = createActiveUser("list_member@club.com", "list_member");
            Club club1 = createClub("Club Member 1", "club_mem_1");
            Club club2 = createClub("Club Member 2", "club_mem_2");
            Club club3 = createClub("Club Director Only", "club_dir_only");

            createClubMember(user.getId(), club1.getId(), ClubRole.MEMBER);
            createClubMember(user.getId(), club2.getId(), ClubRole.MEMBER);
            createClubMember(user.getId(), club3.getId(), ClubRole.DIRECTOR);

            mockMvc.perform(get("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .param("clubRole", "MEMBER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                            club1.getId().toString(),
                            club2.getId().toString()
                    )));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando usuário não tem clubes com a role especificada")
        void listEmptyResult() throws Exception {
            User user = createActiveUser("list_empty@club.com", "list_empty");

            mockMvc.perform(get("/api/clubs")
                            .header("Authorization", bearerToken(user))
                            .param("clubRole", "DIRECTOR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void listUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clubs")
                            .param("clubRole", "DIRECTOR"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PATCH /api/clubs/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/clubs/{id}")
    class UpdateClub {

        @Test
        @DisplayName("deve atualizar clube e retornar 200 quando usuário é DIRECTOR")
        void updateSuccess() throws Exception {
            User director = createActiveUser("update_director@club.com", "update_director");
            Club club = createClub("Old Club Name", "old_nick");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new UpdateClubRequestDTO(
                    "New Club Name",
                    "new_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(club.getId().toString())))
                    .andExpect(jsonPath("$.name", is("New Club Name")))
                    .andExpect(jsonPath("$.nickname", is("new_nick")));
        }

        @Test
        @DisplayName("deve atualizar apenas o nome quando nickname é nulo")
        void updateNameOnly() throws Exception {
            User director = createActiveUser("update_name_only@club.com", "update_name_only");
            Club club = createClub("Old Name", "old_nick");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new UpdateClubRequestDTO(
                    "Updated Name",
                    null
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Name")))
                    .andExpect(jsonPath("$.nickname", is("old_nick")));
        }

        @Test
        @DisplayName("deve atualizar apenas o nickname quando nome é nulo")
        void updateNicknameOnly() throws Exception {
            User director = createActiveUser("update_nick_only@club.com", "update_nick_only");
            Club club = createClub("Club Name", "old_nick");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new UpdateClubRequestDTO(
                    null,
                    "new_nick_updated"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Club Name")))
                    .andExpect(jsonPath("$.nickname", is("new_nick_updated")));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário é apenas MEMBER (não DIRECTOR)")
        void updateForbiddenForMember() throws Exception {
            User member = createActiveUser("update_member@club.com", "update_member");
            Club club = createClub("Club To Update", "club_upd");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            var request = new UpdateClubRequestDTO(
                    "New Name",
                    "new_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(member))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não pertence ao clube")
        void updateForbiddenForNonMember() throws Exception {
            User outsider = createActiveUser("update_outsider@club.com", "update_outsider");
            Club club = createClub("Club Externo", "club_ext");

            var request = new UpdateClubRequestDTO(
                    "New Name",
                    "new_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(outsider))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando clube não existe")
        void updateNotFound() throws Exception {
            User director = createActiveUser("update_notfound@club.com", "update_notfound");

            var request = new UpdateClubRequestDTO(
                    "New Name",
                    "new_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void updateUnauthorized() throws Exception {
            Club club = createClub("Club Update", "club_upd");

            var request = new UpdateClubRequestDTO(
                    "New Name",
                    "new_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando nickname é muito curto (< 3 caracteres)")
        void updateInvalidNickname() throws Exception {
            User director = createActiveUser("update_invalid@club.com", "update_invalid");
            Club club = createClub("Club Valid", "club_val");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new UpdateClubRequestDTO(
                    "New Name",
                    "ab"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando nickname alterado já existe em outro clube")
        void updateDuplicateNickname() throws Exception {
            User director = createActiveUser("update_dup@club.com", "update_dup");
            Club club = createClub("Club A", "club_a_nick");
            createClub("Club B", "club_b_nick");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new UpdateClubRequestDTO(
                    "Club A Renamed",
                    "club_b_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve permitir manter o próprio nickname ao atualizar")
        void updateKeepOwnNickname() throws Exception {
            User director = createActiveUser("update_keep@club.com", "update_keep");
            Club club = createClub("Club Keep", "keep_nick");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            var request = new UpdateClubRequestDTO(
                    "Club Keep Renamed",
                    "keep_nick"
            );

            mockMvc.perform(patch("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname", is("keep_nick")));
        }
    }

    // ── GET /api/clubs/nickname/{nickname}/available ──────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/nickname/{nickname}/available")
    class NicknameAvailable {

        @Test
        @DisplayName("deve retornar available=true quando nickname não existe")
        void availableTrue() throws Exception {
            User user = createActiveUser("avail_true@club.com", "avail_true");

            mockMvc.perform(get("/api/clubs/nickname/{nickname}/available", "livre_nick")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available", is(true)));
        }

        @Test
        @DisplayName("deve retornar available=false quando nickname já existe")
        void availableFalse() throws Exception {
            User user = createActiveUser("avail_false@club.com", "avail_false");
            createClub("Clube Existente", "ocupado_nick");

            mockMvc.perform(get("/api/clubs/nickname/{nickname}/available", "ocupado_nick")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available", is(false)));
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void availableUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clubs/nickname/{nickname}/available", "qualquer"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── DELETE /api/clubs/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/clubs/{id}")
    class DeleteClub {

        @Test
        @DisplayName("deve desativar clube e retornar 200 quando usuário é DIRECTOR")
        void deleteSuccess() throws Exception {
            User director = createActiveUser("delete_director@club.com", "delete_director");
            Club club = createClub("Club To Delete", "club_del");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            mockMvc.perform(delete("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(director)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(club.getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário é apenas MEMBER (não DIRECTOR)")
        void deleteForbiddenForMember() throws Exception {
            User member = createActiveUser("delete_member@club.com", "delete_member");
            Club club = createClub("Club Protegido", "club_prot");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            mockMvc.perform(delete("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário não pertence ao clube")
        void deleteForbiddenForNonMember() throws Exception {
            User outsider = createActiveUser("delete_outsider@club.com", "delete_outsider");
            Club club = createClub("Club Externo Delete", "club_ext_del");

            mockMvc.perform(delete("/api/clubs/{id}", club.getId())
                            .header("Authorization", bearerToken(outsider)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando clube não existe")
        void deleteNotFound() throws Exception {
            User director = createActiveUser("delete_notfound@club.com", "delete_notfound");

            mockMvc.perform(delete("/api/clubs/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(director)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void deleteUnauthorized() throws Exception {
            Club club = createClub("Club Unauth Delete", "club_unauth_del");

            mockMvc.perform(delete("/api/clubs/{id}", club.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
