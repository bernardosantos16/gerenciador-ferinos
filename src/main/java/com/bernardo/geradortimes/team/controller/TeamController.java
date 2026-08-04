package com.bernardo.geradortimes.team.controller;

import com.bernardo.geradortimes.team.dto.request.CreateTeamRequestDTO;
import com.bernardo.geradortimes.team.dto.request.GenerateTeamsRequestDTO;
import com.bernardo.geradortimes.team.dto.request.UpdateTeamJerseyRequestDTO;
import com.bernardo.geradortimes.team.dto.request.UpdateTeamRequestDTO;
import com.bernardo.geradortimes.team.dto.request.SwapPlayersRequestDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
    public Page<TeamResponseDTO> list(
            @Parameter(
                    description = "Filtra por ID da partida.",
                    required = true,
                    example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
            )
            @RequestParam UUID matchId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return teamService.list(matchId, pageable);
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

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar time")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time atualizado.",
                    content = @Content(schema = @Schema(implementation = TeamResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Time ou partida nao encontrados.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TeamResponseDTO update(
            @Parameter(description = "ID do time.", required = true, example = "200")
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequestDTO request
    ) {
        return teamService.update(id, request);
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

    @PostMapping("/swap")
    @Operation(
            summary = "Trocar jogadores entre times",
            description = """
                    Permite trocar jogadores entre times de uma partida.
                    
                    Validações:
                    - Requer que o usuario autenticado seja DIRECTOR do clube da partida.
                    - Os times já devem ter sido gerados para a partida.
                    - Apenas jogadores da mesma posição podem ser trocados (LINE com LINE, GOAL com GOAL).
                    - Jogadores devem estar atribuidos a diferentes times.
                    - Ambos os jogadores devem estar atribuidos (nao podem ter teamId nulo).
                    
                    Retorna os times atualizados com scores recalculados, permitindo
                    que o frontend atualize a UI sem precisar de refresh.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Jogadores trocados com sucesso. Retorna os times atualizados.",
                    content = @Content(schema = @Schema(implementation = GenerateTeamsResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Regra de negocio/validacao (ex.: times nao gerados, posicoes diferentes, etc.).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Partida nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public GenerateTeamsResponseDTO swapPlayers(
            @Valid @RequestBody SwapPlayersRequestDTO request
    ) {
        return teamService.swapPlayers(request);
    }
}