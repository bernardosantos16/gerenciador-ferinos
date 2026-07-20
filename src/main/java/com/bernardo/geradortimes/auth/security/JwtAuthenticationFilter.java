package com.bernardo.geradortimes.auth.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("Requisicao sem token Bearer - seguindo sem autenticacao");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring("Bearer ".length()).trim();
        try {
            DecodedJWT jwt = jwtService.verify(token);

            UUID userId = UUID.fromString(jwt.getSubject());
            String roleValue = jwt.getClaim("role").asString();
            UserRole role = roleValue == null ? UserRole.USER : UserRole.valueOf(roleValue);
            String statusValue = jwt.getClaim("status").asString();
            ActivityStatus status = statusValue == null ? ActivityStatus.ACTIVE : ActivityStatus.valueOf(statusValue);

            if (status != ActivityStatus.ACTIVE) {
                log.warn("Acesso negado - usuario nao ativo - userId: {}, currentStatus: {}, requiredStatus: {}",
                        userId, status, ActivityStatus.ACTIVE);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            UserPrincipal principal = new UserPrincipal(userId, userId.toString(), role, status);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Requisicao autenticada via JWT - userId: {}", userId);
            filterChain.doFilter(request, response);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            log.warn("Autenticacao JWT falhou - failureReason: {}", e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
