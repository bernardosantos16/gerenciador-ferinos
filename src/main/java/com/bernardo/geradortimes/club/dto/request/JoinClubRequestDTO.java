package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public record JoinClubRequestDTO(
        @Schema(
                description = "Token de convite do clube (6 caracteres alfanumericos maiusculos). Obrigatorio quando a politica de ingresso e INVITE_ONLY.",
                example = "A1B2C3"
        )
        @Pattern(regexp = "^[A-Z0-9]{6}$", message = "token de convite invalido")
        String token
) {
}
