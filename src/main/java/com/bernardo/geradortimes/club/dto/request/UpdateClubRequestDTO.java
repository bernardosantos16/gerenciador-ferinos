package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateClubRequestDTO(

        @Schema(
                description = "Novo nome do clube.",
                example = "Ferino FC",
                minLength = 1
        )
        String name,

        @Schema(
                description = "Novo apelido do clube.",
                example = "ferino",
                minLength = 3,
                maxLength = 24,
                nullable = true
        )
        @Size(min = 3, max = 24)
        String nickname
) {
}
