package com.bernardo.geradortimes.shared.observability;

/**
 * Utilitario para sanitizar dados sensiveis (PII) antes de escreve-los em log.
 * <p>
 * Nunca deve ser usado para mascarar segredos como senhas, tokens ou hashes:
 * esses valores simplesmente nao devem ser logados.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * Mascara um e-mail preservando o primeiro caractere da parte local e o dominio,
     * de forma a manter alguma rastreabilidade sem expor o endereco completo.
     * <p>
     * Exemplos: {@code bernardo@dominio.com} -> {@code b***@dominio.com};
     * {@code a@x.com} -> {@code *@x.com}; entrada nula/invalida -> {@code "***"}.
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return "***";
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return "***";
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        if (local.length() <= 1) {
            return "*" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }
}
