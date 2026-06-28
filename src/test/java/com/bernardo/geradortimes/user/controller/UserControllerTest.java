package com.bernardo.geradortimes.user.controller;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.shared.enums.UserRole;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.request.SendEmailTokenRequestDTO;
import com.bernardo.geradortimes.user.dto.request.ResetPasswordRequestDTO;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.model.VerificationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link UserController}.
 * <p>
 * Covers: criar usuario, buscar por ID, listar (ADMIN), deletar, verificar email,
 * solicitar recuperacao de senha e redefinir senha.
 */
@DisplayName("UserController – Integration Tests")
class UserControllerTest extends IntegrationTestBase {

    // ── POST /api/users ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {

        @Test
        @DisplayName("deve criar usuario e retornar 201 (endpoint publico)")
        void createSuccess() throws Exception {
            var request = new CreateUserRequestDTO(
                    "Joao Teste", "joao_teste", "joao@example.com", "Senha@1234");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())))
                    .andExpect(jsonPath("$.nickname", is("joao_teste")))
                    .andExpect(jsonPath("$.login", is("joao@example.com")));
        }

        @Test
        @DisplayName("deve retornar 409 quando o login ja existe")
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
        @DisplayName("deve retornar 409 quando o nickname ja existe")
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
        @DisplayName("deve retornar 400 quando o body e invalido (senha curta)")
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
        @DisplayName("deve retornar 200 quando o usuario busca a si mesmo")
        void getByIdSelf() throws Exception {
            User user = createActiveUser("self@example.com", "self_nick");

            mockMvc.perform(get("/api/users/{id}", user.getId())
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(user.getId().toString())))
                    .andExpect(jsonPath("$.login", is("self@example.com")));
        }

        @Test
        @DisplayName("deve retornar 403 quando um usuario tenta buscar outro usuario sem ser ADMIN")
        void getByIdForbidden() throws Exception {
            User requester = createActiveUser("req@example.com", "req_nick");
            User target    = createActiveUser("target@example.com", "target_nick");

            mockMvc.perform(get("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(requester)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 200 quando ADMIN busca qualquer usuario")
        void getByIdAdmin() throws Exception {
            User admin  = createAdminUser("admin@example.com", "admin_nick");
            User target = createActiveUser("target2@example.com", "target2_nick");

            mockMvc.perform(get("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(target.getId().toString())));
        }

        @Test
        @DisplayName("deve retornar 404 quando o usuario nao existe")
        void getByIdNotFound() throws Exception {
            User admin = createAdminUser("admin2@example.com", "admin2_nick");

            mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando nao autenticado")
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
        @DisplayName("deve retornar 403 quando usuario comum tenta listar todos")
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
        @DisplayName("deve retornar 204 quando ADMIN deleta um usuario")
        void deleteAsAdmin() throws Exception {
            User admin  = createAdminUser("admin4@example.com", "admin4_nick");
            User target = createActiveUser("del@example.com", "del_nick");

            mockMvc.perform(delete("/api/users/{id}", target.getId())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isNoContent());

            assertFalse(userRepository.existsById(target.getId()));
        }

        @Test
        @DisplayName("deve retornar 404 quando o usuario a deletar nao existe")
        void deleteNotFound() throws Exception {
            User admin = createAdminUser("admin5@example.com", "admin5_nick");

            mockMvc.perform(delete("/api/users/{id}", UUID.randomUUID())
                            .header("Authorization", bearerToken(admin.getId(), admin.getLogin().getValue(), UserRole.ADMIN)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuario comum tenta deletar outro usuario")
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
        @DisplayName("deve retornar 204 e ativar o usuario quando o token e valido")
        void verifyEmailSuccess() throws Exception {
            User user = User.create(
                    "Pending User",
                    com.bernardo.geradortimes.shared.value_object.Nickname.of("pending_nick"),
                    com.bernardo.geradortimes.shared.value_object.Email.of("pending@example.com"),
                    com.bernardo.geradortimes.shared.value_object.PasswordHash.fromEncoded(
                            "$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder")
            );
            userRepository.save(user);
            String token = createVerificationToken(user);

            mockMvc.perform(get("/api/users/verify-email")
                            .param("token", token))
                    .andExpect(status().isNoContent());

            User updated = userRepository.findByLogin_Value("pending@example.com").orElseThrow();
            assertEquals(ActivityStatus.ACTIVE, updated.getStatus());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token nao existe")
        void verifyEmailInvalidToken() throws Exception {
            mockMvc.perform(get("/api/users/verify-email")
                            .param("token", "000000"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/users/resend-verification ────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/resend-verification")
    class ResendVerification {

        @Test
        @DisplayName("deve retornar 204 e gerar novo token quando usuario esta PENDING")
        void resendVerificationSuccess() throws Exception {
            User user = User.create(
                    "Pending User",
                    com.bernardo.geradortimes.shared.value_object.Nickname.of("resend_pending"),
                    com.bernardo.geradortimes.shared.value_object.Email.of("resend@example.com"),
                    com.bernardo.geradortimes.shared.value_object.PasswordHash.fromEncoded(
                            "$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder")
            );
            userRepository.save(user);
            var request = new SendEmailTokenRequestDTO("resend@example.com");

            mockMvc.perform(post("/api/users/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 204 sem gerar token quando o usuario ja esta ACTIVE")
        void resendVerificationAlreadyActive() throws Exception {
            User user = createActiveUser("active_resend@example.com", "active_resend");
            var request = new SendEmailTokenRequestDTO("active_resend@example.com");

            mockMvc.perform(post("/api/users/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Nenhum token ACCOUNT_VERIFICATION deve ter sido criado
            var tokens = verificationTokenRepository.findAll().stream()
                    .filter(vt -> vt.getType() == TokenType.ACCOUNT_VERIFICATION
                            && vt.getUserId().equals(user.getId()))
                    .toList();
            assertEquals(0, tokens.size());
        }

        @Test
        @DisplayName("deve retornar 204 mesmo quando o email nao existe (por seguranca)")
        void resendVerificationEmailNotFound() throws Exception {
            var request = new SendEmailTokenRequestDTO("nonexistent@example.com");

            mockMvc.perform(post("/api/users/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve ignorar chamadas repetidas dentro da janela de expiracao (rate limit)")
        void resendVerificationRateLimit() throws Exception {
            User user = User.create(
                    "Pending RateLimit",
                    com.bernardo.geradortimes.shared.value_object.Nickname.of("resend_ratelimit"),
                    com.bernardo.geradortimes.shared.value_object.Email.of("resend_rl@example.com"),
                    com.bernardo.geradortimes.shared.value_object.PasswordHash.fromEncoded(
                            "$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder")
            );
            userRepository.save(user);
            var request = new SendEmailTokenRequestDTO("resend_rl@example.com");

            // Primeira chamada
            mockMvc.perform(post("/api/users/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Segunda chamada — ignorada
            mockMvc.perform(post("/api/users/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Apenas 1 token deve ter sido persistido
            var tokens = verificationTokenRepository.findAll().stream()
                    .filter(vt -> vt.getType() == TokenType.ACCOUNT_VERIFICATION
                            && vt.getUserId().equals(user.getId()))
                    .toList();
            assertEquals(1, tokens.size(), "Deve existir apenas 1 token ACCOUNT_VERIFICATION para o usuario");
        }
    }

    // ── POST /api/users/forgot-password ───────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("deve retornar 204 quando o email existe")
        void forgotPasswordSuccess() throws Exception {
            User user = createActiveUser("forgot@example.com", "forgot_nick");
            var request = new SendEmailTokenRequestDTO("forgot@example.com");

            mockMvc.perform(post("/api/users/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 204 mesmo quando o email nao existe (por seguranca)")
        void forgotPasswordEmailNotFound() throws Exception {
            var request = new SendEmailTokenRequestDTO("nonexistent@example.com");

            mockMvc.perform(post("/api/users/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 400 quando o email e invalido")
        void forgotPasswordInvalidEmail() throws Exception {
            var request = new SendEmailTokenRequestDTO("invalid-email");

            mockMvc.perform(post("/api/users/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve ignorar chamadas repetidas dentro da janela de expiracao (rate limit)")
        void forgotPasswordRateLimit() throws Exception {
            User user = createActiveUser("ratelimit@example.com", "ratelimit_nick");
            var request = new SendEmailTokenRequestDTO("ratelimit@example.com");

            // Primeira chamada — deve criar o token
            mockMvc.perform(post("/api/users/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Segunda chamada — deve ser ignorada silenciosamente (token ainda ativo)
            mockMvc.perform(post("/api/users/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Apenas 1 token deve ter sido persistido
            var passwordResetTokens = verificationTokenRepository.findAll().stream()
                    .filter(vt -> vt.getType() == TokenType.PASSWORD_RESET && vt.getUserId().equals(user.getId()))
                    .toList();
            assertEquals(1, passwordResetTokens.size(), "Deve existir apenas 1 token PASSWORD_RESET para o usuario");
        }
    }

    // ── POST /api/users/reset-password ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("deve retornar 204 e alterar a senha quando o token e valido")
        void resetPasswordSuccess() throws Exception {
            User user = createActiveUser("reset@example.com", "reset_nick");
            String token = createPasswordResetToken(user);
            var request = new ResetPasswordRequestDTO(token, "N0v@S3nh4F0rt3!");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token nao existe")
        void resetPasswordInvalidToken() throws Exception {
            var request = new ResetPasswordRequestDTO("000000", "N0v@S3nh4F0rt3!");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando o token ja foi usado")
        void resetPasswordTokenAlreadyUsed() throws Exception {
            User user = createActiveUser("usedtoken@example.com", "usedtoken_nick");
            String token = createPasswordResetToken(user);

            // Marca o token como usado manualmente
            var vt = verificationTokenRepository
                    .findByTokenHashAndType(
                            sha256(token),
                            com.bernardo.geradortimes.shared.enums.TokenType.PASSWORD_RESET)
                    .orElseThrow();
            vt.markUsed();
            verificationTokenRepository.save(vt);

            var request = new ResetPasswordRequestDTO(token, "N0v@S3nh4F0rt3!");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 401 quando o token expirou")
        void resetPasswordExpiredToken() throws Exception {
            User user = createActiveUser("expired@example.com", "expired_nick");
            String token = String.valueOf(100000 + new java.security.SecureRandom().nextInt(900000));
            String hash = sha256(token);

            // Cria um token expirado (-1 minuto de expiracao)
            VerificationToken vt = VerificationToken.create(
                    hash,
                    com.bernardo.geradortimes.shared.enums.TokenType.PASSWORD_RESET,
                    Instant.now().minus(1, ChronoUnit.MINUTES),
                    user.getId()
            );
            verificationTokenRepository.save(vt);

            var request = new ResetPasswordRequestDTO(token, "N0v@S3nh4F0rt3!");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando a nova senha e muito curta")
        void resetPasswordInvalidBody() throws Exception {
            User user = createActiveUser("shortpw@example.com", "shortpw_nick");
            String token = createPasswordResetToken(user);
            var request = new ResetPasswordRequestDTO(token, "123");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
