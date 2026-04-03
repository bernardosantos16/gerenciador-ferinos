package com.bernardo.geradortimes.match.controller;

import com.bernardo.geradortimes.match.dto.request.CreateMatchRequestDTO;
import com.bernardo.geradortimes.match.dto.response.MatchParticipantResponseDTO;
import com.bernardo.geradortimes.match.dto.response.MatchResponseDTO;
import com.bernardo.geradortimes.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches", description = "Criacao e consulta de partidas.")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @Operation(
            summary = "Criar partida",
            description = "Cria uma partida associada a um clube. Requer que o usuario autenticado seja DIRECTOR do clube."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Partida criada.",
                    content = @Content(schema = @Schema(implementation = MatchResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MatchResponseDTO> create(@Valid @RequestBody CreateMatchRequestDTO request) {
        MatchResponseDTO created = matchService.create(request);
        return ResponseEntity
                .created(URI.create("/api/matches/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar partida por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partida encontrada.",
                    content = @Content(schema = @Schema(implementation = MatchResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (MEMBER requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Partida nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public MatchResponseDTO getById(
            @Parameter(description = "ID da partida.", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID id
    ) {
        return matchService.getById(id);
    }

    @GetMapping
    @Operation(
            summary = "Listar partidas por clube",
            description = "Lista as partidas de um clube. Requer que o usuario autenticado seja MEMBER do clube."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de partidas.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MatchResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (MEMBER requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Page<MatchResponseDTO> listByClub(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @RequestParam UUID clubId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return matchService.listByClub(clubId, pageable);
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Listar participantes de uma partida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de participantes.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MatchParticipantResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (MEMBER requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Partida nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<MatchParticipantResponseDTO> listParticipants(
            @Parameter(description = "ID da partida.", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID id
    ) {
        return matchService.listParticipants(id);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir partida",
            description = "Remove a partida e dados dependentes (participantes e times). Requer DIRECTOR do clube."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Partida excluida."),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Partida nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID da partida.", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID id
    ) {
        matchService.delete(id);
    }
}
