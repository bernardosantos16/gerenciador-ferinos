package com.bernardo.geradortimes.auth.controller;

import com.bernardo.geradortimes.auth.dto.request.LoginRequestDTO;
import com.bernardo.geradortimes.auth.dto.request.LogoutRequestDTO;
import com.bernardo.geradortimes.auth.dto.request.RefreshTokenRequestDTO;
import com.bernardo.geradortimes.auth.dto.response.TokenResponseDTO;
import com.bernardo.geradortimes.auth.service.AuthCookieService;
import com.bernardo.geradortimes.auth.service.AuthService;
import com.bernardo.geradortimes.auth.service.AuthTokens;
import com.bernardo.geradortimes.auth.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Fluxos de autenticacao e tokens (JWT).")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            AuthCookieService authCookieService
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Autentica via email/senha e retorna `accessToken` (JWT). "
                    + "O `refreshToken` (opaco) e definido em cookie HttpOnly e Secure."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado.",
                    content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        AuthTokens tokens = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieService.buildRefreshTokenCookie(tokens.refreshToken()).toString());
        return new TokenResponseDTO(tokens.accessToken(), "Bearer", tokens.expiresInSeconds());
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar Access Token",
            description = "Rotaciona o refresh token e retorna um novo access token. "
                    + "Aceita o refresh token no corpo ou em cookie HttpOnly."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados.",
                    content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido/expirado/revogado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TokenResponseDTO refresh(
            @CookieValue(name = "${auth.cookie.refresh-token-name:refreshToken}", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh token required");
        }
        AuthTokens tokens = authService.refresh(new RefreshTokenRequestDTO(refreshTokenCookie));
        response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieService.buildRefreshTokenCookie(tokens.refreshToken()).toString());
        return new TokenResponseDTO(tokens.accessToken(), "Bearer", tokens.expiresInSeconds());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout (Revogar Refresh Token)",
            description = "Revoga um refresh token. Operacao idempotente: se o token nao existir, ainda retorna 204. "
                    + "Aceita o refresh token no corpo ou em cookie HttpOnly."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refresh token revogado (ou inexistente)."),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody(required = false) LogoutRequestDTO request,
            @CookieValue(name = "${auth.cookie.refresh-token-name:refreshToken}", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(request, refreshTokenCookie);
        if (refreshToken != null) {
            refreshTokenService.revokeByTokenValue(refreshToken);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshTokenCookie().toString());
    }

    private static String resolveRefreshToken(RefreshTokenRequestDTO request, String refreshTokenCookie) {
        if (request != null) {
            return request.refreshToken();
        }
        return refreshTokenCookie;
    }

    private static String resolveRefreshToken(LogoutRequestDTO request, String refreshTokenCookie) {
        if (request != null) {
            return request.refreshToken();
        }
        return refreshTokenCookie;
    }
}
