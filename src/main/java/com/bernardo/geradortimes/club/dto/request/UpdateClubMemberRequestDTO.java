package com.bernardo.geradortimes.club.dto.request;

import com.bernardo.geradortimes.shared.enums.ClubRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateClubMemberRequestDTO(
        @Schema(
                description = "Novo nome do membro.",
                example = "Joao",
                maxLength = 250
        )
        @Size(max = 250)
        String name,

        @Schema(
                description = "Novo nivel/nota do jogador (1 a 5).",
                example = "4",
                minimum = "1",
                maximum = "5"
        )
        @Min(1) @Max(5)
        Integer rating,

        @Schema(
                description = "Nova role do membro no clube.",
                example = "DIRECTOR"
        )
        ClubRole clubRole
) {
}
