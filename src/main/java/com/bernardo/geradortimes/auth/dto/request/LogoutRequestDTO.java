package com.bernardo.geradortimes.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequestDTO(
        @Schema(
                description = "Refresh token a ser revogado. Se nao existir, a operacao e idempotente (retorna 204).",
                example = "pLZV8FvK9c0mUe6aGq5m6rZq3m2GgTn4cX... (base64url)",
                maxLength = 255
        )
        @NotBlank
        @Size(max = 255)
        String refreshToken
) {
}
