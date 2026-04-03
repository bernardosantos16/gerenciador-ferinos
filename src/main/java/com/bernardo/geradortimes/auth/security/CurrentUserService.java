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
            log.error("Não autorizado, não autenticado");
            throw new ResponseStatusException(UNAUTHORIZED, "unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.id();
        }
        log.error("Não autorizado, autenticação não aponta usuário");
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
