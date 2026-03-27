package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddClubMemberRequestDTO(
        @Schema(
                description = "Nome do membro do clube (pode ser um participante sem usuario cadastrado).",
                example = "Joao"
        )
        @NotBlank
        @Size(max = 250)
        String name,

        @Schema(
                description = "Nivel/nota do jogador (1 a 5). Usado como parte do score na geracao automatica de times.",
                example = "3",
                minimum = "1",
                maximum = "5"
        )
        @Min(1) @Max(5)
        Integer rating

) {
}
