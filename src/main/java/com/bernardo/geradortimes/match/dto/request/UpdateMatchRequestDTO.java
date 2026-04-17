package com.bernardo.geradortimes.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateMatchRequestDTO(
        @Schema(
                description = "Nova data e hora da partida (UTC).",
                type = "string",
                format = "date-time",
                example = "2026-03-21T22:00:00Z"
        )
        @NotNull
        @FutureOrPresent
        Instant dateTime
) {
}
