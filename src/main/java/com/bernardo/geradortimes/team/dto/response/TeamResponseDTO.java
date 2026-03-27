package com.bernardo.geradortimes.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TeamResponseDTO(
        @Schema(description = "ID do time.", example = "200", format = "int64")
        Long id,

        @Schema(description = "ID da partida.", format = "uuid", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID matchId,

        @Schema(description = "ID da camisa do clube usada pelo time.", example = "10", format = "int64", nullable = true)
        Long clubJerseyId
) {
}
