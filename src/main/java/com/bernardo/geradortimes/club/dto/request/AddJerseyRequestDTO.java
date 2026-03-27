package com.bernardo.geradortimes.club.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddJerseyRequestDTO(
        @Schema(description = "Nome da camisa.", example = "Camisa Azul", maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(
                description = "Cor em HEX no formato `#RRGGBB` (o `#` e opcional).",
                example = "#1E90FF",
                pattern = "^#?[0-9a-fA-F]{6}$"
        )
        @NotBlank
        @Pattern(regexp = "^#?[0-9a-fA-F]{6}$")
        String hexColor,

        @Schema(
                description = "Se `true`, indica que esta camisa e destinada ao goleiro.",
                example = "false"
        )
        @NotNull
        Boolean isGoalkeeperJersey
) {
}
