package com.bernardo.geradortimes.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequestDTO(
        @Schema(description = "Email a ser verificado.", example = "joao@example.com", maxLength = 100)
        @NotBlank
        @Email
        String login,

        @Schema(description = "Token de verificacao enviado por email (6 digitos).", example = "123456")
        @NotBlank
        String token
) {
}
