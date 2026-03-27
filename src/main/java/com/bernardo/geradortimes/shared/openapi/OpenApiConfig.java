package com.bernardo.geradortimes.shared.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Gerador de Times API",
                description = """
                        API para gerenciar usuarios, clubes, partidas e times, incluindo a geracao automatica de times.

                        Autenticacao:
                        - Use o endpoint de login para obter um `accessToken` (JWT).
                        - Envie o header `Authorization: Bearer <token>` nas rotas protegidas.
                        """,
                version = "v1",
                contact = @Contact(name = "Gerador de Times")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT no header Authorization. Exemplo: `Authorization: Bearer eyJhbGciOi...`."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // Keep servers minimal to avoid locking the spec to a single environment.
        return new OpenAPI().servers(List.of(new Server().url("/").description("Default")));
    }
}

