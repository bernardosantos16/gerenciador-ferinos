package com.bernardo.geradortimes.club.controller;


import com.bernardo.geradortimes.club.dto.request.AddClubMemberRequestDTO;
import com.bernardo.geradortimes.club.service.ClubMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}/members")
@Tag(name = "Club Members", description = "Membros de um clube (jogadores), com ou sem usuario cadastrado.")
@SecurityRequirement(name = "bearerAuth")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    public ClubMemberController(ClubMemberService clubMemberService) {
        this.clubMemberService = clubMemberService;
    }

    @PostMapping
    @Operation(summary = "Adicionar membro ao clube (sem usuario)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membro criado."),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> add(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Valid @RequestBody AddClubMemberRequestDTO request
    ) {
        clubMemberService.addNonUserClubMember(clubId, request);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + clubId + "/members"))
                .build();
    }
}
