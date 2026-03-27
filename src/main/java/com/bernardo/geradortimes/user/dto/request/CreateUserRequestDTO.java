package com.bernardo.geradortimes.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDTO(
        @Schema(description = "Nome completo do usuario.", example = "João Teste", maxLength = 250)
        @NotBlank
        @Size(max = 250)
        String name,

        @Schema(
                description = "Apelido (unico) do usuario. Usado para exibicao. Pode ser uma string simples.",
                example = "joao_teste",
                maxLength = 100
        )
        @NotBlank
        @Size(max = 100)
        String nickname,

        @Schema(description = "Login (email) do usuario. Deve ser unico.", example = "joao@example.com", maxLength = 100)
        @NotBlank
        @Size(max = 100)
        @Email
        String login,

        @Schema(
                description = "Senha em texto puro. Minimo 8 e maximo 72 caracteres.",
                example = "S3nh4F0rt3!",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
