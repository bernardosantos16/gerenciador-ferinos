package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.dto.request.JoinClubRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubMembershipRequestResponseDTO;
import com.bernardo.geradortimes.club.dto.response.InviteTokenResponseDTO;
import com.bernardo.geradortimes.club.service.ClubMembershipService;
import com.bernardo.geradortimes.shared.enums.MembershipRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}")
@Tag(name = "Club Membership", description = "Solicitacao de ingresso em um clube e aprovacao/recusa pelo diretor.")
@SecurityRequirement(name = "bearerAuth")
public class ClubMembershipController {

    private final ClubMembershipService clubMembershipService;

    public ClubMembershipController(ClubMembershipService clubMembershipService) {
        this.clubMembershipService = clubMembershipService;
    }

    @PostMapping("/invite")
    @Operation(summary = "Solicitar ingresso usando token de convite (body)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitacao criada.",
                    content = @Content(schema = @Schema(implementation = ClubMembershipRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Token invalido ou expirado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Clube nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Clube inativo ou usuario ja e membro/pendente.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubMembershipRequestResponseDTO> requestJoinByBody(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Valid @RequestBody(required = false) JoinClubRequestDTO request
    ) {
        String token = request != null ? request.token() : null;
        var response = clubMembershipService.requestJoin(clubId, token);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + clubId + "/membership-requests/" + response.id()))
                .body(response);
    }

    @GetMapping("/invite-token")
    @Operation(summary = "Visualizar token de convite (DIRECTOR)",
            description = "Retorna o token de convite atual (gera automaticamente se nao existir ou estiver expirado).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token obtido.",
                    content = @Content(schema = @Schema(implementation = InviteTokenResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InviteTokenResponseDTO> getInviteToken(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId
    ) {
        return ResponseEntity.ok(clubMembershipService.getInviteToken(clubId));
    }

    @PostMapping("/invite-token")
    @Operation(summary = "Regenerar token de convite (DIRECTOR)",
            description = "Gera um novo token de convite, invalidando o anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token regenerado.",
                    content = @Content(schema = @Schema(implementation = InviteTokenResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InviteTokenResponseDTO> regenerateInviteToken(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId
    ) {
        return ResponseEntity.ok(clubMembershipService.regenerateInviteToken(clubId));
    }

    @GetMapping("/membership-requests")
    @Operation(summary = "Listar solicitacoes de ingresso (DIRECTOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacoes listadas.",
                    content = @Content(schema = @Schema(implementation = ClubMembershipRequestResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Page<ClubMembershipRequestResponseDTO>> listRequests(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "Status das solicitacoes. Default PENDING.")
            @RequestParam(required = false) MembershipRequestStatus status,
            @ParameterObject @PageableDefault(sort = "requestedAt") Pageable pageable
    ) {
        return ResponseEntity.ok(clubMembershipService.listRequests(clubId, status, pageable));
    }

    @PostMapping("/membership-requests/{requestId}/approve")
    @Operation(summary = "Aprovar solicitacao de ingresso (DIRECTOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao aprovada.",
                    content = @Content(schema = @Schema(implementation = ClubMembershipRequestResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Solicitacao ja decidida.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubMembershipRequestResponseDTO> approve(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID da solicitacao.", required = true, example = "1")
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(clubMembershipService.approve(clubId, requestId));
    }

    @PostMapping("/membership-requests/{requestId}/reject")
    @Operation(summary = "Recusar solicitacao de ingresso (DIRECTOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitacao recusada.",
                    content = @Content(schema = @Schema(implementation = ClubMembershipRequestResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui permissao (DIRECTOR requerido).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitacao nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Solicitacao ja decidida.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ClubMembershipRequestResponseDTO> reject(
            @Parameter(description = "ID do clube.", required = true, example = "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f")
            @PathVariable UUID clubId,
            @Parameter(description = "ID da solicitacao.", required = true, example = "1")
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(clubMembershipService.reject(clubId, requestId));
    }
}
