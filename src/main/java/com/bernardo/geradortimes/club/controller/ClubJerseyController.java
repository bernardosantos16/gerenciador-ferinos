package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.dto.request.AddJerseyRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateJerseyRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubJerseyResponseDTO;
import com.bernardo.geradortimes.club.service.ClubJerseyService;
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
@RequestMapping("/api/clubs/{clubId}/jerseys")
@Tag(name = "Club Jerseys", description = "Camisas associadas a um clube.")
@SecurityRequirement(name = "bearerAuth")
public class ClubJerseyController {

    private final ClubJerseyService clubJerseyService;

    public ClubJerseyController(ClubJerseyService clubJerseyService) {
        this.clubJerseyService = clubJerseyService;
    }

    @PostMapping
    @Operation(summary = "Adicionar camisa ao clube")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Camisa criada.",
                    content = @Content(schema = @Schema(implementation = ClubJerseyResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubJerseyResponseDTO> add(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Valid @RequestBody AddJerseyRequestDTO request
    ) {
        ClubJerseyResponseDTO created = clubJerseyService.addJersey(clubId, request);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + clubId + "/jerseys/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Listar camisas do clube (paginado)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de camisas.",
                    content = @Content(schema = @Schema(implementation = ClubJerseyResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (MEMBER requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Page<ClubJerseyResponseDTO> list(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return clubJerseyService.pageByClub(clubId, pageable);
    }

    @PatchMapping("/{jerseyId}")
    @Operation(summary = "Atualizar camisa do clube")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camisa atualizada.",
                    content = @Content(schema = @Schema(implementation = ClubJerseyResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Camisa nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubJerseyResponseDTO> update(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID da camisa.", required = true, example = "10")
            @PathVariable Long jerseyId,
            @Valid @RequestBody UpdateJerseyRequestDTO request
    ) {
        return ResponseEntity.ok(clubJerseyService.updateJersey(clubId, jerseyId, request));
    }

    @DeleteMapping("/{jerseyId}")
    @Operation(summary = "Remover camisa do clube")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Camisa removida."),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Camisa nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID da camisa.", required = true, example = "10")
            @PathVariable Long jerseyId
    ) {
        clubJerseyService.delete(clubId, jerseyId);
    }
}
