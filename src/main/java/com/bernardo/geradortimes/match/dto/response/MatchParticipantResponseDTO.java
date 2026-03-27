package com.bernardo.geradortimes.match.dto.response;

import com.bernardo.geradortimes.shared.enums.MatchParticipantPosition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record MatchParticipantResponseDTO(
        @Schema(description = "ID do registro de participacao.", example = "100", format = "int64")
        Long id,

        @Schema(description = "ID da partida.", format = "uuid", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID matchId,

        @Schema(description = "ID do membro do clube.", example = "12", format = "int64")
        Long clubMemberId,

        @Schema(description = "Posicao do participante na partida.", example = "LINE")
        MatchParticipantPosition position,

        @Schema(
                description = "ID do time atribuido. Pode ser `null` (ex.: goleiro nao atribuido a nenhum time).",
                example = "200",
                format = "int64",
                nullable = true
        )
        Long teamId
) {
}
