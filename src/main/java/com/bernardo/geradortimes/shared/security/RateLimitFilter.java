package com.bernardo.geradortimes.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit por IP para endpoints sensiveis (ex: envio de email).
 * <p>
 * Janela deslizante simples: cada IP pode fazer ate {@code maxRequests} requisicoes
 * dentro de {@code windowSeconds}. Ao estourar, retorna HTTP 429.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Entry> counters = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.max-requests:3}")
    private int maxRequests;

    @Value("${app.rate-limit.window-seconds:60}")
    private long windowSeconds;



    private boolean shouldRateLimit(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals("/api/users/email");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Remove entradas expiradas a cada 5 minutos para evitar memory leak.
     */
    @Scheduled(fixedRate = 300_000)
    public void cleanStaleEntries() {
        long cutoff = System.currentTimeMillis() - (windowSeconds * 1000);
        int before = counters.size();
        counters.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
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
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000);

        Entry entry = counters.compute(ip, (key, current) -> {
            if (current == null || current.windowStart < windowStart) {
                return new Entry(now, 1);
            }
            return new Entry(current.windowStart, current.count + 1);
        });

        if (entry.count > maxRequests) {
            log.warn("Rate limit excedido para IP: {} ({} requisicoes em {}s)", ip, entry.count, windowSeconds);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"title\":\"Too many requests\",\"status\":429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record Entry(long windowStart, int count) {
    }
}
