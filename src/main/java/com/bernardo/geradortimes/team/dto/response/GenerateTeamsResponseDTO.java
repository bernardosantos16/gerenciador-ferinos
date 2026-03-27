package com.bernardo.geradortimes.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record GenerateTeamsResponseDTO(
        @Schema(description = "ID da partida.", format = "uuid", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID matchId,

        @Schema(description = "Quantidade de times gerados.", example = "3", minimum = "1")
        Integer teamCount,

        @Schema(description = "Lista dos times gerados, com jogadores de linha e (opcionalmente) goleiro.")
        List<GeneratedTeamDTO> teams,

        @Schema(
                description = "Goleiros informados na entrada que nao foram atribuidos a nenhum time (ex.: quantidade diferente de teamCount).",
                example = "[101,102]"
        )
        List<Long> unassignedGoalkeeperMemberIds
) {
}
