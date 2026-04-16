package com.bernardo.geradortimes.user.controller;

import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.response.UserResponseDTO;
import com.bernardo.geradortimes.user.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Cadastro e administracao de usuarios.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(
            summary = "Criar usuario",
            description = "Cria um usuario local com login (email) e senha. Endpoint publico.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado.",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida (validacao).",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflito: nickname ou login ja existe.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado.",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public UserResponseDTO getById(
            @Parameter(description = "ID do usuario.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    ) {
        return userService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado.")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponseDTO> list(@PageableDefault(size = 50) Pageable pageable) {
        return userService.list(pageable);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario excluido."),
            @ApiResponse(responseCode = "401", description = "Nao autenticado."),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @Parameter(description = "ID do usuario.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    ) {
        userService.delete(id);
    }

    @GetMapping("/verify-email")
    @Operation(
            summary = "Verificar email",
            description = "Verifica o email do usuario usando o token de verificacao enviado por email. Endpoint publico.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email verificado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Token de verificacao invalido ou expirado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token de verificacao invalido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(
            @Parameter(description = "Token de verificacao de email.", required = true)
            @RequestParam String token
    ) {
        userService.verifyEmailToken(token);
    }
}
