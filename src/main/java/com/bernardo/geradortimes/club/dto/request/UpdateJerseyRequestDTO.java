package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateJerseyRequestDTO(
        @Schema(description = "Novo nome da camisa.", example = "Camisa Vermelha", maxLength = 100)
        @Size(max = 100)
        String name,

        @Schema(
                description = "Nova cor em HEX no formato `#RRGGBB`.",
                example = "#FF0000",
                pattern = "^#?[0-9a-fA-F]{6}$"
        )
        @Pattern(regexp = "^#?[0-9a-fA-F]{6}$")
        String hexColor,

        @Schema(
                description = "Se `true`, indica que esta camisa e destinada ao goleiro.",
                example = "true"
        )
        Boolean isGoalkeeperJersey
) {
}
