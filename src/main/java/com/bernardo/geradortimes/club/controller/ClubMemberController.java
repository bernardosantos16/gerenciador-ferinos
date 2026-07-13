package com.bernardo.geradortimes.club.controller;


import com.bernardo.geradortimes.club.dto.request.AddClubMemberRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateClubMemberRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubMemberResponseDTO;
import com.bernardo.geradortimes.club.model.ClubMember;
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
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Slf4j
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
    public ResponseEntity<ClubMemberResponseDTO> add(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Valid @RequestBody AddClubMemberRequestDTO request
    ) {
        var newMember = clubMemberService.addNonUserClubMember(clubId, request);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + clubId + "/members/" + newMember.id()))
                .body(newMember);
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Buscar membro do clube por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro atualizado.",
                    content = @Content(schema = @Schema(implementation = ClubMemberResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Membro nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubMemberResponseDTO> getMember(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID do membro.", required = true, example = "1")
            @PathVariable Long memberId
    ) {

        return ResponseEntity.ok(clubMemberService.getMember(clubId, memberId));
    }

    @GetMapping
    @Operation(summary = "Listar membros do clube")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membros listados.",
                    content = @Content(schema = @Schema(implementation = ClubMemberResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (membro do clube requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Page<ClubMemberResponseDTO>> list(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @ParameterObject @PageableDefault(sort = "name") Pageable pageable
    ) {
        Page<ClubMemberResponseDTO> members = clubMemberService.paginateMembers(clubId, pageable);
        return ResponseEntity.ok(members);
    }

    @PatchMapping("/{memberId}")
    @Operation(summary = "Atualizar membro do clube")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro atualizado.",
                    content = @Content(schema = @Schema(implementation = ClubMemberResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Membro nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubMemberResponseDTO> update(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID do membro.", required = true, example = "1")
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateClubMemberRequestDTO request
    ) {
        log.info("Atualizando membro {} do clube {}", memberId, clubId);
        return ResponseEntity.ok(clubMemberService.updateMember(clubId, memberId, request));
    }

    @DeleteMapping("/{memberId}")
    @Operation(summary = "Remover membro do clube")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido."),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Membro nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID do membro.", required = true, example = "1")
            @PathVariable Long memberId
    ) {
        clubMemberService.removeMember(clubId, memberId);
    }
}
