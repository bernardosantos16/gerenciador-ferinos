package com.bernardo.geradortimes.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTeamRequestDTO(
        @Schema(description = "ID da partida.", format = "uuid", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @NotNull
        UUID matchId,

        @Schema(description = "ID da camisa do clube usada pelo time.", example = "10", format = "int64")
        @NotNull
        Long clubJerseyId
) {
}
