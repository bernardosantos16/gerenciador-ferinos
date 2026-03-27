package com.bernardo.geradortimes.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record UserResponseDTO(
        @Schema(description = "ID do usuario.", format = "uuid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nome completo do usuario.", example = "Bernardo Silva")
        String name,

        @Schema(description = "Apelido do usuario.", example = "bernardo")
        String nickname,

        @Schema(description = "Login (email) do usuario.", example = "bernardo@example.com")
        String login
) {
}
