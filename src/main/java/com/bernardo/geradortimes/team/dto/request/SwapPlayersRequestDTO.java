package com.bernardo.geradortimes.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SwapPlayersRequestDTO(
        @Schema(
                description = "ID da partida onde os jogadores serão trocados.",
                format = "uuid",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        )
        @NotNull
        UUID matchId,

        @Schema(
                description = "Lista de trocas de jogadores. Cada elemento contém os IDs de dois membros que trocarão de time.",
                example = "[{\"memberIdFrom\": 1, \"memberIdTo\": 2}, {\"memberIdFrom\": 3, \"memberIdTo\": 4}]"
        )
        @NotNull
        List<PlayerSwapDTO> swaps
) {
}

