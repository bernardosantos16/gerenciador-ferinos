package com.bernardo.geradortimes.shared.security;

import com.bernardo.geradortimes.auth.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit por IP+endpoint para endpoints sensiveis (login, refresh, registro, envio/verificacao de
 * email, recuperacao/redefinicao de senha, geracao de times).
 * <p>
 * Cada endpoint tem seu proprio balde de contagem, isolando budgets. Janela deslizante simples:
 * cada IP pode fazer ate {@code maxRequests} requisicoes por endpoint dentro de {@code windowSeconds}.
 * Ao estourar o limite do endpoint, retorna HTTP 429.
 * <p>
 * Valores padrao e por endpoint configurados via {@code app.rate-limit.*}.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_URIS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/users",
            "/api/users/email",
            "/api/users/verify-email",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/teams/generate",
            "/api/clubs/invite"
    );

    private final RateLimitProperties properties;
    private final Map<String, Entry> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && PROTECTED_URIS.contains(canonicalUri(request.getRequestURI()));
    }

    /**
     * Normaliza URIs dinamicas para um caminho canonico usado no rate limit.
     * Ex.: {@code /api/clubs/{clubId}/invite} vira {@code /api/clubs/invite},
     * evitando que o clubId fragmente o contador.
     */
    private String canonicalUri(String uri) {
        String[] parts = uri.split("/");
        if (parts.length >= 5
                && "api".equals(parts[1])
                && "clubs".equals(parts[2])
                && "invite".equals(parts[4])) {
            return "/api/clubs/invite";
        }
        return uri;
    }

    private String getClientIp(HttpServletRequest request) {
        // Em prod, server.forward-headers-strategy=framework faz o ForwardedHeaderFilter
        // reescrever getRemoteAddr() a partir do proxy confiavel. Nao confiamos no
        // header X-Forwarded-For enviado pelo proprio cliente.
        return request.getRemoteAddr();
    }

    /**
     * Remove entradas expiradas a cada 5 minutos para evitar memory leak.
     * Cada entrada usa sua propria janela de expiracao, permitindo endpoints com
     * windowSeconds diferentes coexistirem no mesmo mapa.
     */
    @Scheduled(fixedRate = 300_000)
    public void cleanStaleEntries() {
        long now = System.currentTimeMillis();
        int before = counters.size();
        counters.entrySet().removeIf(e -> {
            Entry entry = e.getValue();
            return entry.windowStart < (now - entry.windowSeconds * 1000);
        });
        log.debug("Limpeza de contadores de rate limit executada - antes: {}, depois: {}", before, counters.size());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!shouldRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        String uri = canonicalUri(request.getRequestURI());
        String key = ip + "|" + uri;
        long now = System.currentTimeMillis();

        long windowSeconds = properties.windowSecondsFor(uri);
        int maxRequests = properties.maxRequestsFor(uri);
        long windowStart = now - (windowSeconds * 1000);

        Entry entry = counters.compute(key, (k, current) -> {
            if (current == null || current.windowStart < windowStart) {
                return new Entry(now, 1, windowSeconds);
            }
            return new Entry(current.windowStart, current.count + 1, windowSeconds);
        });

        if (entry.count > maxRequests) {
            log.warn("Rate limit excedido para IP {} no endpoint {} ({} requisicoes em {}s)",
                    ip, uri, entry.count, windowSeconds);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"title\":\"Too many requests\",\"status\":429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record Entry(long windowStart, int count, long windowSeconds) {
    }
}
