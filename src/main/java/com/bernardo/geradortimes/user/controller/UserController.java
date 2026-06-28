package com.bernardo.geradortimes.user.controller;

import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.user.dto.request.SendEmailTokenRequestDTO;
import com.bernardo.geradortimes.user.dto.request.ResetPasswordRequestDTO;
import com.bernardo.geradortimes.user.dto.response.UserResponseDTO;
import com.bernardo.geradortimes.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(
            summary = "Criar um novo usuario",
            description = "Endpoint publico para cadastro de um novo usuario. O status inicial e PENDING e um email de verificacao e enviado.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos (ex: senha curta, email invalido)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Login ou nickname ja estao em uso", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO response = userService.create(request);
        URI location = URI.create("/api/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Buscar usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario comum tentando acessar outro usuario", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Listar usuarios (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de usuarios"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario comum tentando listar", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.list(pageable));
    }

    @Operation(summary = "Deletar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario deletado"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario comum tentando deletar outro usuario", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Verificar email via token",
            description = "Endpoint publico. Recebe um token de 6 digitos e ativa a conta do usuario.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email verificado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Token invalido ou expirado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        userService.verifyEmailToken(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reenviar email de verificacao",
            description = "Endpoint publico. Reenvia o token de verificacao para o email informado. "
                    + "Ignorado silenciosamente se o email nao for encontrado, ja estiver ativo ou ja possuir um token ativo.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitacao processada"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody SendEmailTokenRequestDTO request) {
        userService.resendVerification(request.login());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Solicitar recuperacao de senha",
            description = "Endpoint publico. Envia um token de recuperacao de senha por email.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token enviado (mesmo se o email nao existir, por seguranca)"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody SendEmailTokenRequestDTO request) {
        userService.forgotPassword(request.login());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Redefinir senha com token",
            description = "Endpoint publico. Recebe o token de recuperacao e a nova senha.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Token invalido ou expirado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        userService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

}
