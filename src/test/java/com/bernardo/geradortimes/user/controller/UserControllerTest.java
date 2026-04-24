package com.bernardo.geradortimes.user.controller;

import com.bernardo.geradortimes.shared.enums.UserRole;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
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
 * Integration tests for {@link UserController}.
 * <p>
 * Covers: criar usuário, buscar por ID, listar (ADMIN), deletar e verificar email.
 */
@DisplayName("UserController – Integration Tests")
class UserControllerTest extends IntegrationTestBase {

    // ── POST /api/users ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {

        @Test
        @DisplayName("deve criar usuário e retornar 201 (endpoint público)")
        void createSuccess() throws Exception {
            var request = new CreateUserRequestDTO(
                    "João Teste", "joao_teste", "joao@example.com", "Senha@1234");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())))
                    .andExpect(jsonPath("$.nickname", is("joao_teste")))
                    .andExpect(jsonPath("$.login", is("joao@example.com")));
        }

        @Test
        @DisplayName("deve retornar 409 quando o login já existe")
        void createDuplicateLogin() throws Exception {
            createActiveUser("dup@example.com", "dup_nick");

            var request = new CreateUserRequestDTO(
                    "Outro Nome", "outro_nick", "dup@example.com", "Senha@1234");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 409 quando o nickname já existe")
        void createDuplicateNickname() throws Exception {
            createActiveUser("first@example.com", "same_nick");

            var request = new CreateUserRequestDTO(
                    "Outro Nome", "same_nick", "second@example.com", "Senha@1234");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 400 quando o body é inválido (senha curta)")
        void createInvalidBody() throws Exception {
            var request = new CreateUserRequestDTO(
                    "Nome", "nick_val", "valid@example.com", "123");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/users/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 quando o usuário busca a si mesmo")
        void getByIdSelf() throws Exception {
            User user = createActiveUser("self@example.com", "self_nick");

            mockMvc.perform(get("/api/users/{id}", user.getId())
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(user.getId().toString())))
                    .andExpect(jsonPath("$.login", is("self@example.com")));
        }

        @Test
        @DisplayName("deve retornar 403 quando um usuário tenta buscar outro usuário sem ser ADMIN")
        void getByIdForbidden() throws Exception {
            User requester = createActiveUser("req@example.com", "req_nick");
            User target    = createActiveUser("target@example.com", "target_nick");

            mockMvc.perform(get("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(requester)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 200 quando ADMIN busca qualquer usuário")
        void getByIdAdmin() throws Exception {
            User admin  = createAdminUser("admin@example.com", "admin_nick");
            User target = createActiveUser("target2@example.com", "target2_nick");

            mockMvc.perform(get("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(target.getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 404 quando o usuário não existe")
        void getByIdNotFound() throws Exception {
            User admin = createAdminUser("admin2@example.com", "admin2_nick");

            mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void getByIdUnauthorized() throws Exception {
            User user = createActiveUser("unauth@example.com", "unauth_nick");

            mockMvc.perform(get("/api/users/{id}", user.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/users ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/users")
    class ListUsers {

        @Test
        @DisplayName("deve retornar 200 com lista paginada quando ADMIN")
        void listAsAdmin() throws Exception {
            User admin = createAdminUser("admin3@example.com", "admin3_nick");
            createActiveUser("u1@example.com", "u1_nick");
            createActiveUser("u2@example.com", "u2_nick");

            mockMvc.perform(get("/api/users")
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário comum tenta listar todos")
        void listForbiddenForRegularUser() throws Exception {
            User user = createActiveUser("regular@example.com", "regular_nick");

            mockMvc.perform(get("/api/users")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("deve retornar 204 quando ADMIN deleta um usuário")
        void deleteAsAdmin() throws Exception {
            User admin  = createAdminUser("admin4@example.com", "admin4_nick");
            User target = createActiveUser("del@example.com", "del_nick");

            mockMvc.perform(delete("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isNoContent());

            org.junit.jupiter.api.Assertions.assertFalse(userRepository.existsById(target.getId()));
        }

        @Test
        @DisplayName("deve retornar 404 quando o usuário a deletar não existe")
        void deleteNotFound() throws Exception {
            User admin = createAdminUser("admin5@example.com", "admin5_nick");

            mockMvc.perform(delete("/api/users/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário comum tenta deletar outro usuário")
        void deleteForbidden() throws Exception {
            User requester = createActiveUser("req2@example.com", "req2_nick");
            User target    = createActiveUser("target3@example.com", "target3_nick");

            mockMvc.perform(delete("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(requester)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/users/verify-email ───────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/users/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("deve retornar 204 e ativar o usuário quando o token é válido")
        void verifyEmailSuccess() throws Exception {
            // Create a PENDING user with a verification token
            User user = User.create(
                    "Pending User",
                    com.bernardo.geradortimes.shared.value_object.Nickname.of("pending_nick"),
                    com.bernardo.geradortimes.shared.value_object.Email.of("pending@example.com"),
                    com.bernardo.geradortimes.shared.value_object.PasswordHash.fromEncoded(
                            "$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder")
            );
            String token = user.generateEmailVerificationToken();
            userRepository.save(user);

            mockMvc.perform(get("/api/users/verify-email")
                            .param("token", token))
                    .andExpect(status().isNoContent());

            User updated = userRepository.findByLogin_Value("pending@example.com").orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(
                    com.bernardo.geradortimes.shared.enums.ActivityStatus.ACTIVE, updated.getStatus());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token não existe")
        void verifyEmailInvalidToken() throws Exception {
            mockMvc.perform(get("/api/users/verify-email")
                            .param("token", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }
    }
}

