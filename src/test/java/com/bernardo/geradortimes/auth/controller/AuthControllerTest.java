package com.bernardo.geradortimes.auth.controller;

import com.bernardo.geradortimes.auth.dto.request.LoginRequestDTO;
import com.bernardo.geradortimes.auth.repository.RefreshTokenRepository;
import com.bernardo.geradortimes.auth.service.RefreshTokenService;
import com.bernardo.geradortimes.shared.security.PasswordService;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 * <p>
 * Covers: login, refresh token rotation, and logout flows.
 */
@DisplayName("AuthController – Integration Tests")
class AuthControllerTest extends IntegrationTestBase {

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates an ACTIVE user whose password is properly hashed with Argon2.
     */
    private void createUserWithPassword(String login, String nickname, String rawPassword) {
        User user = createActiveUser(login, nickname);
        // Override the placeholder hash with a real Argon2 hash
        try {
            var passwordField = com.bernardo.geradortimes.shared.value_object.PasswordHash.class
                    .getDeclaredField("value");
            passwordField.setAccessible(true);

            var embeddedField = User.class.getDeclaredField("password");
            embeddedField.setAccessible(true);

            var hash = com.bernardo.geradortimes.shared.value_object.PasswordHash.fromEncoded(
                    passwordService.hash(rawPassword));
            embeddedField.set(user, hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userRepository.save(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("deve retornar 200 e accessToken quando credenciais são válidas")
        void loginSuccess() throws Exception {
            createUserWithPassword("user@test.com", "user_test", "Senha@12345");

            String body = toJson(new LoginRequestDTO("user@test.com", "Senha@12345"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", not(emptyString())))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.expiresInSeconds", greaterThan(0)))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("deve retornar 401 quando a senha está incorreta")
        void loginWrongPassword() throws Exception {
            createUserWithPassword("user2@test.com", "user_test2", "Senha@1234");

            String body = toJson(new LoginRequestDTO("user2@test.com", "SenhaErrada!"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 401 quando o usuário não existe")
        void loginUserNotFound() throws Exception {
            String body = toJson(new LoginRequestDTO("naoexiste@test.com", "Senha@1234"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando o body é inválido (email malformado)")
        void loginInvalidBody() throws Exception {
            String body = toJson(new LoginRequestDTO("nao-e-email", "Senha@1234"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 401 quando o usuário está INATIVO")
        void loginInactiveUser() throws Exception {
            createUserWithPassword("inativo@test.com", "inativo_test", "Senha@1234");
            User user = userRepository.findByLogin_Value("inativo@test.com").orElseThrow();
            user.inactivateUser();
            userRepository.save(user);

            String body = toJson(new LoginRequestDTO("inativo@test.com", "Senha@1234"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("deve retornar 200 e novo accessToken quando o refreshToken é válido (via cookie)")
        void refreshSuccess() throws Exception {
            createUserWithPassword("refresh@test.com", "refresh_test", "Senha@1234");
            User user = userRepository.findByLogin_Value("refresh@test.com").orElseThrow();

            String rawRefreshToken = refreshTokenService.issue(user.getId());

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", not(emptyString())))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("deve retornar 400 quando nenhum refreshToken é fornecido")
        void refreshMissingToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 401 quando o refreshToken é inválido")
        void refreshInvalidToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", "token-invalido")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 401 quando o refreshToken está revogado")
        void refreshRevokedToken() throws Exception {
            createUserWithPassword("revoked@test.com", "revoked_test", "Senha@1234");
            User user = userRepository.findByLogin_Value("revoked@test.com").orElseThrow();

            String rawRefreshToken = refreshTokenService.issue(user.getId());
            refreshTokenService.revokeByTokenValue(rawRefreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("deve retornar 204 e revogar o refreshToken via cookie")
        void logoutViaCookie() throws Exception {
            createUserWithPassword("logout@test.com", "logout_test", "Senha@1234");
            User user = userRepository.findByLogin_Value("logout@test.com").orElseThrow();
            String rawRefreshToken = refreshTokenService.issue(user.getId());

            mockMvc.perform(post("/api/auth/logout")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawRefreshToken)))
                    .andExpect(status().isNoContent());

            // Verify token is revoked in DB
            var tokenOpt = refreshTokenService.findByTokenValue(rawRefreshToken);
            org.junit.jupiter.api.Assertions.assertTrue(tokenOpt.isPresent());
            org.junit.jupiter.api.Assertions.assertTrue(tokenOpt.get().isRevoked());
        }

        @Test
        @DisplayName("deve retornar 204 mesmo quando nenhum token é fornecido (idempotente)")
        void logoutNoToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());
        }
    }
}

