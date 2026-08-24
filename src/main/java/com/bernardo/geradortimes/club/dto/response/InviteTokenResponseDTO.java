package com.bernardo.geradortimes.club.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record InviteTokenResponseDTO(
        @Schema(description = "Token de convite em texto puro (exibido apenas na geracao).", example = "qW7Z1xR4...")
        String token,

        @Schema(description = "Data/hora de expiracao do token.", example = "2026-09-01T12:00:00Z")
        Instant expiresAt
) {
}
