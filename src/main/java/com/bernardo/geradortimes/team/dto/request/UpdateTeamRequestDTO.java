package com.bernardo.geradortimes.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateTeamRequestDTO(
        @Schema(description = "Novo ID de camisa do clube para o time.", example = "11", format = "int64")
        @NotNull
        Long clubJerseyId
) {
}
