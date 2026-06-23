package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.dto.request.CreateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubResponseDTO;
import com.bernardo.geradortimes.club.service.ClubService;
import com.bernardo.geradortimes.shared.enums.ClubRole;
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
import java.util.List;
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

    @GetMapping("nickname/{nickname}")
    @Operation(summary = "Buscar clube por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clube encontrado.",
                    content = @Content(schema = @Schema(implementation = ClubResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Clube nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubResponseDTO> getClubByNickName(
            @Parameter(description = "nickname do clube.", required = true, example = "club_fc")
            @PathVariable String nickname
    ){
        var response = clubService.getClubByNickname(nickname);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar clubes do usuario por funcao",
            description = "Lista os clubes do usuario autenticado, filtrando por funcao (MEMBER, DIRECTOR)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clubes obtida.",
                    content = @Content(schema = @Schema(implementation = ClubResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Clube nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<ClubResponseDTO>> getClubsByRole(@RequestParam ClubRole clubRole) {
        var response = clubService.listUserClubs(clubRole);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar clube")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clube atualizado.",
                    content = @Content(schema = @Schema(implementation = ClubResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Clube nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubResponseDTO> update(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClubRequestDTO request
    ) {
        return ResponseEntity.ok(clubService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desativar clube (soft delete)",
            description = "Altera o status do clube para INACTIVE. Requer DIRECTOR do clube."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clube desativado.",
                    content = @Content(schema = @Schema(implementation = ClubResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Clube nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubResponseDTO> delete(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(clubService.softDelete(id));
    }
}
