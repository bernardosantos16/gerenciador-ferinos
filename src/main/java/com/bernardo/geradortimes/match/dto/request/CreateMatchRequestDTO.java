package com.bernardo.geradortimes.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateMatchRequestDTO(
        @Schema(description = "ID do clube dono da partida.", format = "uuid", example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
        @NotNull
        UUID clubId,

        @Schema(
                description = "Data e hora da partida (UTC).",
                type = "string",
                format = "date-time",
                example = "2026-03-20T22:00:00Z"
        )
        @NotNull
        @FutureOrPresent
        Instant dateTime
) {
}
