package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateClubRequestDTO(

        @Schema(
                description = "Nome do clube.",
                example = "Ferino FC",
                minLength = 1
        )
        @NotNull @NotBlank
        String name,

        @Schema(
                description = "Apelido do clube para exibicao.",
                example = "ferino",
                minLength = 3,
                maxLength = 24,
                nullable = false
        )
        @NotBlank
        @Pattern(regexp = "^[a-z0-9_-]{3,24}$", message = "nickname deve ter de 3 a 24 caracteres (minúsculas, números, _ ou -)")
        String nickname
) {
}
