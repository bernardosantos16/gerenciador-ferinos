package com.bernardo.geradortimes.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @Schema(description = "Email do usuario.", example = "joao@example.com", maxLength = 100)
        @NotBlank
        @Email
        String email,

        @Schema(description = "Token de recuperacao de senha enviado por email.", example = "123456")
        @NotBlank
        String token,

        @Schema(
                description = "Nova senha em texto puro. Minimo 8 e maximo 72 caracteres.",
                example = "N0v@S3nh4!",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
