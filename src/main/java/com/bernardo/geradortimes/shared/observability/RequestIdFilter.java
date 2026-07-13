package com.bernardo.geradortimes.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gera (ou reaproveita) um identificador de correlacao por requisicao e o injeta
 * no MDC do SLF4J sob a chave {@value #MDC_KEY}, permitindo correlacionar todas as
 * linhas de log emitidas durante o ciclo de vida da requisicao.
 * <p>
 * O id e devolvido no header de resposta {@value #HEADER} para permitir correlacao
 * do lado do cliente. Se o cliente enviar um {@value #HEADER} valido, ele e
 * reaproveitado; caso contrario, um novo UUID e gerado.
 * <p>
 * O MDC e limpo no {@code finally} para nao vazar entre requisicoes que compartilham
 * threads reaproveitadas do pool do Tomcat.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_LENGTH = 64;
    private static final Pattern SAFE_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER);
        if (incoming != null) {
            String trimmed = incoming.trim();
            if (!trimmed.isEmpty()
                    && trimmed.length() <= MAX_LENGTH
                    && SAFE_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }
}
