package com.bernardo.geradortimes.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendEmailTokenRequestDTO(
        @Schema(description = "Email (login) do usuario que deseja enviar o token.", example = "joao@example.com", maxLength = 100)
        @NotBlank
        @Size(max = 100)
        @Email
        String login
) {
}
