package com.bernardo.geradortimes.auth.security;

import com.bernardo.geradortimes.shared.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
public class CurrentUserService {

    public UUID requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Acesso nao autorizado - requisicao sem autenticacao");
            throw new ResponseStatusException(UNAUTHORIZED, "unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.id();
        }
        log.warn("Acesso nao autorizado - autenticacao nao aponta um usuario valido");
        throw new ResponseStatusException(UNAUTHORIZED, "unauthorized");
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.role() == UserRole.ADMIN;
        }
        return false;
    }
}
