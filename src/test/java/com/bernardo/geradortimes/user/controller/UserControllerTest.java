package com.bernardo.geradortimes.user.controller;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.TokenType;
import com.bernardo.geradortimes.shared.enums.UserRole;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.request.ResetPasswordRequestDTO;
import com.bernardo.geradortimes.user.dto.request.SendEmailTokenRequestDTO;
import com.bernardo.geradortimes.user.dto.request.VerifyEmailRequestDTO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link UserController}.
 * <p>
 * Covers: verificacao de email, confirmacao de email, criar usuario, buscar por ID,
 * listar (ADMIN), deletar, solicitar recuperacao de senha e redefinir senha.
 */
@DisplayName("UserController – Integration Tests")
class UserControllerTest extends IntegrationTestBase {

    // ── POST /api/users/email ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/email")
    class SendEmailVerification {

        @Test
        @DisplayName("deve retornar 204 quando o email nao esta cadastrado")
        void sendEmailVerificationSuccess() throws Exception {
            var request = new SendEmailTokenRequestDTO("novo@example.com");

            mockMvc.perform(post("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 409 quando o email ja esta cadastrado")
        void sendEmailVerificationAlreadyRegistered() throws Exception {
            createActiveUser("exist@example.com", "exist_nick");
            var request = new SendEmailTokenRequestDTO("exist@example.com");

            mockMvc.perform(post("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 400 quando o email e invalido")
        void sendEmailVerificationInvalidEmail() throws Exception {
            var request = new SendEmailTokenRequestDTO("invalid-email");

            mockMvc.perform(post("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve ignorar chamadas repetidas dentro da janela de expiracao")
        void sendEmailVerificationRateLimit() throws Exception {
            var request = new SendEmailTokenRequestDTO("ratelimit@example.com");

            // Primeira chamada
            mockMvc.perform(post("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Segunda chamada — ignorada
            mockMvc.perform(post("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Apenas 1 token deve ter sido persistido
            var tokens = verificationTokenRepository.findAll().stream()
                    .filter(vt -> vt.getType() == TokenType.EMAIL_VERIFICATION
                            && vt.getEmail().equals("ratelimit@example.com"))
                    .toList();
            assertEquals(1, tokens.size());
        }
    }

    // ── POST /api/users/verify-email ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("deve retornar 200 com registrationToken quando o OTP e valido e consumi-lo")
        void verifyEmailSuccess() throws Exception {
            String token = createVerificationToken("verify@example.com");
            var request = new VerifyEmailRequestDTO("verify@example.com", token);

            mockMvc.perform(post("/api/users/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationToken", not(emptyString())));

            // Verifica que o OTP foi consumido
            var consumed = verificationTokenRepository
                    .findByTokenHashAndTypeAndEmail(sha256(token), TokenType.EMAIL_VERIFICATION, "verify@example.com");
            assertTrue(consumed.isPresent());
            assertNotNull(consumed.get().getUsedAt());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token nao existe")
        void verifyEmailInvalidToken() throws Exception {
            var request = new VerifyEmailRequestDTO("nope@example.com", "000000");

            mockMvc.perform(post("/api/users/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 quando o OTP ja foi consumido")
        void verifyEmailTokenAlreadyUsed() throws Exception {
            String email = "used_otp@example.com";
            String token = createVerificationToken(email);

            // Consome o token na primeira chamada
            var request1 = new VerifyEmailRequestDTO(email, token);
            mockMvc.perform(post("/api/users/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request1)))
                    .andExpect(status().isOk());

            // Segunda chamada com mesmo OTP deve falhar
            var request2 = new VerifyEmailRequestDTO(email, token);
            mockMvc.perform(post("/api/users/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request2)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /api/users ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {

        private static CreateUserRequestDTO buildRequest(String name, String nickname, String password, String registrationToken) {
            return new CreateUserRequestDTO(name, nickname, password, registrationToken);
        }

        @Test
        @DisplayName("deve criar usuario com status ACTIVE e retornar 201")
        void createSuccess() throws Exception {
            String registrationJwt = createRegistrationJwt("joao@example.com");
            var request = buildRequest("Joao Teste", "joao_teste", "Senha@1234", registrationJwt);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())))
                    .andExpect(jsonPath("$.nickname", is("joao_teste")))
                    .andExpect(jsonPath("$.login", is("joao@example.com")));

            User user = userRepository.findByLogin_Value("joao@example.com").orElseThrow();
            assertEquals(ActivityStatus.ACTIVE, user.getStatus());
        }

        @Test
        @DisplayName("deve retornar 409 quando o login ja existe")
        void createDuplicateLogin() throws Exception {
            createActiveUser("dup@example.com", "dup_nick");

            String registrationJwt = createRegistrationJwt("dup@example.com");
            var request = buildRequest("Outro Nome", "outro_nick", "Senha@1234", registrationJwt);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 409 quando o nickname ja existe")
        void createDuplicateNickname() throws Exception {
            createActiveUser("first@example.com", "same_nick");

            String registrationJwt = createRegistrationJwt("second@example.com");
            var request = buildRequest("Outro Nome", "same_nick", "Senha@1234", registrationJwt);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 400 quando o body e invalido (senha curta)")
        void createInvalidBody() throws Exception {
            String registrationJwt = createRegistrationJwt("valid@example.com");
            var request = buildRequest("Nome", "nick_val", "123", registrationJwt);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token de registro e invalido")
        void createInvalidToken() throws Exception {
            var request = buildRequest("Nome", "nick_val", "Senha@1234", "token-invalido");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token de registro e de outro tipo (access token)")
        void createWrongTokenPurpose() throws Exception {
            User user = createActiveUser("purpose@example.com", "purpose_nick");
            String accessToken = bearerToken(user).replace("Bearer ", "");

            var request = buildRequest("Nome", "nick_purpose", "Senha@1234", accessToken);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
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
                    .filter(vt -> vt.getType() == TokenType.PASSWORD_RESET
                            && vt.getEmail().equals("ratelimit@example.com"))
                    .toList();
            assertEquals(1, passwordResetTokens.size(), "Deve existir apenas 1 token PASSWORD_RESET para o email");
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
            var request = new ResetPasswordRequestDTO("reset@example.com", token, "N0v@S3nh4F0rt3!");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando o token nao existe")
        void resetPasswordInvalidToken() throws Exception {
            var request = new ResetPasswordRequestDTO("nope@example.com", "000000", "N0v@S3nh4F0rt3!");

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
                    .findByTokenHashAndTypeAndEmail(
                            sha256(token),
                            TokenType.PASSWORD_RESET,
                            "usedtoken@example.com")
                    .orElseThrow();
            vt.markUsed();
            verificationTokenRepository.save(vt);

            var request = new ResetPasswordRequestDTO("usedtoken@example.com", token, "N0v@S3nh4F0rt3!");

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
                    TokenType.PASSWORD_RESET,
                    Instant.now().minus(1, ChronoUnit.MINUTES),
                    "expired@example.com"
            );
            verificationTokenRepository.save(vt);

            var request = new ResetPasswordRequestDTO("expired@example.com", token, "N0v@S3nh4F0rt3!");

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
            var request = new ResetPasswordRequestDTO("shortpw@example.com", token, "123");

            mockMvc.perform(post("/api/users/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
