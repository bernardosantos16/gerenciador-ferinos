package com.bernardo.geradortimes.club.dto.response;

import com.bernardo.geradortimes.shared.enums.JoinPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ClubResponseDTO(
        @Schema(description = "ID do clube.", format = "uuid", example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
        UUID id,

        @Schema(description = "Nome do clube.", example = "Ferino FC")
        String name,

        @Schema(description = "Apelido do clube.", example = "ferino")
        String nickname,

        @Schema(description = "Politica de ingresso do clube.", example = "INVITE_ONLY")
        JoinPolicy joinPolicy
) {
}
