package com.bernardo.geradortimes.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record VerifyEmailResponseDTO(
        @Schema(description = "JWT de registro para ser usado na etapa de criacao da conta.", example = "eyJ...")
        String registrationToken
) {
}
