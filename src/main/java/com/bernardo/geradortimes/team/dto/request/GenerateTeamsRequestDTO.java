package com.bernardo.geradortimes.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GenerateTeamsRequestDTO(
        @Schema(description = "ID da partida onde os times serao gerados.", format = "uuid", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @NotNull
        UUID matchId,

        @Schema(
                description = "Lista de IDs de membros do clube que jogarao na linha.",
                example = "[1,2,3,4,5,6,7,8,9,10]"
        )
        @NotNull
        List<Long> lineMemberIds,

        @Schema(
                description = "Lista de IDs de membros do clube que jogarao no gol. Se o tamanho for diferente do numero de times, os goleiros ficam como nao-atribuidos.",
                example = "[101,102,103]"
        )
        @NotNull
        List<Long> goalkeeperMemberIds,

        @Schema(
                description = "Quantidade maxima de jogadores de linha por time (>= 1).",
                example = "5",
                minimum = "1"
        )
        @NotNull
        @Min(1)
        Integer maxLinePlayers
) {
}
