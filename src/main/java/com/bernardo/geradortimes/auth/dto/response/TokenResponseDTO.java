package com.bernardo.geradortimes.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponseDTO(
        @Schema(
                description = "JWT de acesso. Use no header `Authorization: Bearer <token>`.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI... (JWT)"
        )
        String accessToken,

        @Schema(
                description = "Tipo do token para o header Authorization.",
                example = "Bearer"
        )
        String tokenType,

        @Schema(
                description = "Tempo de vida do access token em segundos.",
                example = "900",
                minimum = "1"
        )
        long expiresInSeconds
) {
}
