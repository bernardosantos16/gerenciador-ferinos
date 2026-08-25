package com.bernardo.geradortimes.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

public record CreateMatchBatchRequestDTO(
        @Schema(description = "ID do clube dono das partidas.", format = "uuid", example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
        @NotNull
        UUID clubId,

        @Schema(description = "Dia da semana em que as partidas serao criadas.", example = "TUESDAY")
        @NotNull
        DayOfWeek dayOfWeek,

        @Schema(
                description = "Horario das partidas (ex: 20:00). Sera usado em todas as partidas do lote.",
                type = "string",
                format = "time",
                example = "20:00"
        )
        @NotNull
        LocalTime time,

        @Schema(
                description = "Data de inicio do intervalo. Partidas serao criadas a partir desta data (inclusive).",
                type = "string",
                format = "date",
                example = "2026-07-01"
        )
        @NotNull
        LocalDate startDate,

        @Schema(
                description = "Data de fim do intervalo. Partidas serao criadas ate esta data (inclusive).",
                type = "string",
                format = "date",
                example = "2026-07-31"
        )
        @NotNull
        LocalDate endDate,

        @Schema(
                description = "Fuso horario para interpretar a data e hora. Ex: America/Sao_Paulo, UTC.",
                example = "America/Sao_Paulo"
        )
        @NotNull
        ZoneId zoneId
) {
}
