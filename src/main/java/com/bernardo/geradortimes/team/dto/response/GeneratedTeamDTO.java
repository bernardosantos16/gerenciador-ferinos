package com.bernardo.geradortimes.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GeneratedTeamDTO(
        @Schema(description = "ID do time gerado/persistido.", example = "200", format = "int64")
        Long teamId,

        @Schema(description = "IDs dos membros alocados como jogadores de linha.", example = "[1,2,3,4,5]")
        List<Long> lineMemberIds,

        @Schema(
                description = "ID do membro alocado como goleiro. Pode ser `null` se nao houve goleiro atribuido.",
                example = "101",
                format = "int64",
                nullable = true
        )
        Long goalkeeperMemberId,

        @Schema(description = "Score total do time, soma dos scores normalizados de todos os membros (linha + goleiro).", example = "4.25")
        double totalScore
) {
}
