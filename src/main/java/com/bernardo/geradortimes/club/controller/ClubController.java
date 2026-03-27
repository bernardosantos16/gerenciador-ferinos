package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.dto.request.CreateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubResponseDTO;
import com.bernardo.geradortimes.club.service.ClubService;
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
@RequestMapping("/api/clubs")
@Tag(name = "Clubs", description = "Operacoes de clubes.")
@SecurityRequirement(name = "bearerAuth")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping
    @Operation(
            summary = "Criar clube",
            description = "Cria um clube e registra o usuario autenticado como DIRECTOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Clube criado.",
                    content = @Content(schema = @Schema(implementation = ClubResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado.")
    })
    public ResponseEntity<ClubResponseDTO> create(@Valid @RequestBody CreateClubRequestDTO request) {
        ClubResponseDTO created = clubService.createClub(request);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar clube por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clube encontrado.",
                    content = @Content(schema = @Schema(implementation = ClubResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Clube nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubResponseDTO> getClub(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID id
    ){
        var response = clubService.getClub(id);
        return ResponseEntity.ok(response);
    }
}
