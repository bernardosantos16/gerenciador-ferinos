package com.bernardo.geradortimes.club.dto.request;

import com.bernardo.geradortimes.shared.enums.JoinPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public record UpdateClubRequestDTO(

        @Schema(
                description = "Novo nome do clube.",
                example = "Ferino FC",
                minLength = 1
        )
        String name,

        @Schema(
                description = "Novo apelido do clube. Vazio ou nulo mantém o atual.",
                example = "ferino",
                minLength = 3,
                maxLength = 24,
                nullable = true
        )
        @Pattern(regexp = "^$|[a-z0-9_-]{3,24}$", message = "nickname deve ter de 3 a 24 caracteres (minúsculas, números, _ ou -)")
        String nickname,

        @Schema(
                description = "Politica de ingresso do clube (OPEN ou INVITE_ONLY). Nulo mantem a atual.",
                example = "INVITE_ONLY",
                nullable = true
        )
        JoinPolicy joinPolicy
) {
}
