package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClubRequestDTO(

        @Schema(
                description = "Nome do clube.",
                example = "Ferino FC",
                minLength = 1
        )
        @NotNull @NotBlank
        String name,

        @Schema(
                description = "Apelido do clube para exibicao. Se nao informado, o backend usa o `name` como fallback.",
                example = "ferino",
                minLength = 3,
                maxLength = 24,
                nullable = true
        )
        @Size(min = 3, max = 24)
        String nickname
) {
}
