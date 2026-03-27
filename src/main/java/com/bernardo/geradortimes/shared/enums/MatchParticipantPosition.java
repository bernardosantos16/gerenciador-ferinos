package com.bernardo.geradortimes.shared.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Posicao do participante na partida.",
        allowableValues = {"LINE", "GOAL"}
)
public enum MatchParticipantPosition {
    LINE,
    GOAL
}
