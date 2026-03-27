package com.bernardo.geradortimes.club.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ClubJerseyResponseDTO(
        @Schema(description = "ID da camisa.", example = "10", format = "int64")
        Long id,

        @Schema(description = "Nome da camisa.", example = "Camisa Azul")
        String name,

        @Schema(description = "Cor em HEX.", example = "#1E90FF", pattern = "^#?[0-9a-fA-F]{6}$")
        String hexColor,

        @Schema(description = "Indica se e uma camisa de goleiro.", example = "false")
        Boolean isGoalkeeperJersey,

        @Schema(description = "ID do clube dono da camisa.", format = "uuid", example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
        UUID clubId
) {
}
