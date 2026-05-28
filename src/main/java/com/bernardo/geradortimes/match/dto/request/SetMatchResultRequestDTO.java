package com.bernardo.geradortimes.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SetMatchResultRequestDTO(
        @Schema(description = "ID do time campeao da partida.", example = "1")
        @NotNull
        @Positive
        Long teamChampionId,

        @Schema(description = "ID do membro escolhido como MVP da partida.", example = "10")
        @NotNull
        @Positive
        Long clubMemberMvpId
) {
}
