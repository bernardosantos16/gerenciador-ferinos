package com.bernardo.geradortimes.match.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record MatchResponseDTO(
        @Schema(description = "ID da partida.", format = "uuid", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID id,

        @Schema(description = "ID do clube dono da partida.", format = "uuid", example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
        UUID clubId,

        @Schema(type = "string", format = "date-time", description = "Data e hora da partida (UTC).", example = "2026-03-20T22:00:00Z")
        Instant dateTime
) {
}
