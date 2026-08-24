package com.bernardo.geradortimes.notification.controller;

import com.bernardo.geradortimes.notification.dto.response.NotificationResponseDTO;
import com.bernardo.geradortimes.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notificacoes do usuario autenticado.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Listar notificacoes do usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacoes listadas.",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado.")
    })
    public ResponseEntity<Page<NotificationResponseDTO>> list(
            @Parameter(description = "Se true, retorna apenas nao lidas. Default false.")
            @RequestParam(required = false) Boolean unread,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.list(unread, pageable));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar notificacao como lida")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificacao marcada como lida."),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Notificacao nao encontrada.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> markRead(
            @Parameter(description = "ID da notificacao.", required = true, example = "1")
            @PathVariable Long id
    ) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }
}
