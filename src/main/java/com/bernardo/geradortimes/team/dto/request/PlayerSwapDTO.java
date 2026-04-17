package com.bernardo.geradortimes.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PlayerSwapDTO(
        @Schema(
                description = "ID do primeiro membro participante da troca.",
                example = "1"
        )
        @NotNull
        Long memberIdFrom,

        @Schema(
                description = "ID do segundo membro participante da troca.",
                example = "2"
        )
        @NotNull
        Long memberIdTo
) {
}

