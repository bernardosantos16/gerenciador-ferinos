package com.bernardo.geradortimes.team.controller;

import com.bernardo.geradortimes.team.dto.request.CreateTeamRequestDTO;
import com.bernardo.geradortimes.team.dto.request.GenerateTeamsRequestDTO;
import com.bernardo.geradortimes.team.dto.request.UpdateTeamJerseyRequestDTO;
import com.bernardo.geradortimes.team.dto.response.GenerateTeamsResponseDTO;
import com.bernardo.geradortimes.team.dto.response.TeamResponseDTO;
import com.bernardo.geradortimes.team.service.TeamService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/teams")
@Tag(name = "Teams", description = "CRUD de times e geracao automatica.")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @Operation(summary = "Criar time manualmente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Time criado.",
                    content = @Content(schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado.")
    })
    public ResponseEntity<TeamResponseDTO> create(@Valid @RequestBody CreateTeamRequestDTO request) {
        TeamResponseDTO created = teamService.create(request);
        return ResponseEntity
                .created(URI.create("/api/teams/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar time por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time encontrado.",
                    content = @Content(schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Time nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TeamResponseDTO getById(
            @Parameter(description = "ID do time.", required = true, example = "200")
            @PathVariable Long id
    ) {
        return teamService.getById(id);
    }

    @GetMapping
    @Operation(
            summary = "Listar times",
            description = "Lista todos os times. Se `matchId` for informado, filtra por partida."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de times.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TeamResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado.")
    })
    public List<TeamResponseDTO> list(
            @Parameter(
                    description = "Filtra por ID da partida (opcional).",
                    required = false,
                    example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
            )
            @RequestParam(required = false) UUID matchId
    ) {
        return teamService.list(matchId);
    }

    @PatchMapping("/{id}/jersey")
    @Operation(summary = "Atualizar camisa do time")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camisa atualizada.",
                    content = @Content(schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Time nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TeamResponseDTO updateJersey(
            @Parameter(description = "ID do time.", required = true, example = "200")
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamJerseyRequestDTO request
    ) {
        return teamService.updateJersey(id, request);
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Gerar times automaticamente",
            description = """
                    Gera times balanceados para uma partida:
                    - Distribui jogadores de linha por score (rating + historico) tentando equilibrar os times.
                    - Se a quantidade de goleiros for igual ao numero de times, atribui um goleiro por time.
                    - Caso contrario, todos os goleiros ficam como nao-atribuidos.

                    Requer que o usuario autenticado seja DIRECTOR do clube da partida.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Times gerados.",
                    content = @Content(schema = @Schema(implementation = GenerateTeamsResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Regra de negocio/validacao (ex.: matchId invalido, IDs duplicados, etc.).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public GenerateTeamsResponseDTO generate(@Valid @RequestBody GenerateTeamsRequestDTO request) {
        return teamService.generate(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir time")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Time excluido."),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Time nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID do time.", required = true, example = "200")
            @PathVariable Long id
    ) {
        teamService.delete(id);
    }
}
