package com.bernardo.geradortimes.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.dto.request.CreateUserRequestDTO;
import com.bernardo.geradortimes.auth.dto.request.LoginRequestDTO;
import com.bernardo.geradortimes.user.dto.request.ResetPasswordRequestDTO;
import com.bernardo.geradortimes.user.dto.request.SendEmailTokenRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Teste de regressao de PII em log.
 * <p>
 * Anexa um {@link ListAppender} ao logger da nossa aplicacao
 * ({@value #APPLICATION_LOGGER}), exercita os fluxos mais sensiveis (registro,
 * login, recuperacao de senha) com valores sensiveis conhecidos e garante que
 * nenhum dado sensivel (e-mail completo, senha, token) vaze para os nossos logs.
 * <p>
 * O escopo e restrito ao pacote da aplicacao propositalmente: loggers de
 * frameworks (Spring MVC, Hibernate, driver JDBC) manipulam o corpo bruto da
 * requisicao em niveis TRACE/DEBUG que nao sao habilitados em producao e estao
 * fora do nosso controle. O objetivo do teste e pegar vazamentos no NOSSO codigo
 * (ex: {@code toString()} de DTO, mensagens de excecao, valores rejeitados).
 * <p>
 * O mascaramento de e-mail preserva o dominio (ex: {@code p***@secret-domain.example}),
 * portanto o assert verifica que a parte local do e-mail nunca aparece integralmente.
 */
@DisplayName("Regressao de PII em logs")
class PiiLogRegressionTest extends IntegrationTestBase {

    private static final String APPLICATION_LOGGER = "com.bernardo.geradortimes";

    private static final String SENSITIVE_EMAIL = "pii.victim.local@secret-domain.example";
    private static final String SENSITIVE_LOCAL_PART = "pii.victim.local";
    private static final String SENSITIVE_PASSWORD = "SuperSecretP4ss!word";
    private static final String SENSITIVE_NICKNAME = "pii_secret_nickname";

    private Logger applicationLogger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void attachAppender() {
        applicationLogger = (Logger) LoggerFactory.getLogger(APPLICATION_LOGGER);
        originalLevel = applicationLogger.getLevel();
        applicationLogger.setLevel(Level.DEBUG);

        appender = new ListAppender<>();
        appender.start();
        applicationLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        applicationLogger.detachAppender(appender);
        appender.stop();
        applicationLogger.setLevel(originalLevel);
    }

    @Test
    @DisplayName("fluxo de registro/login/reset nao vaza email completo, senha nem token")
    void sensitiveFlowsDoNotLeakPii() throws Exception {
        // 1. Envio de codigo de verificacao (email sensivel no corpo)
        mockMvc.perform(post("/api/users/email")
                .contentType(APPLICATION_JSON)
                .content(toJson(new SendEmailTokenRequestDTO(SENSITIVE_EMAIL))));

        // 2. Login com credenciais invalidas (email + senha sensiveis no corpo)
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(toJson(new LoginRequestDTO(SENSITIVE_EMAIL, SENSITIVE_PASSWORD))));

        // 3. Criacao de usuario com token de registro valido (senha + token sensiveis)
        String registrationToken = createRegistrationJwt(SENSITIVE_EMAIL);
        mockMvc.perform(post("/api/users")
                .contentType(APPLICATION_JSON)
                .content(toJson(new CreateUserRequestDTO(
                        "PII Victim",
                        SENSITIVE_NICKNAME,
                        SENSITIVE_PASSWORD,
                        registrationToken))));

        // 4. Recuperacao de senha para email inexistente (email sensivel no corpo)
        mockMvc.perform(post("/api/users/forgot-password")
                .contentType(APPLICATION_JSON)
                .content(toJson(new SendEmailTokenRequestDTO(SENSITIVE_EMAIL))));

        // 5. Reset de senha com token invalido (email + senha + token sensiveis)
        mockMvc.perform(post("/api/users/reset-password")
                .contentType(APPLICATION_JSON)
                .content(toJson(new ResetPasswordRequestDTO(SENSITIVE_EMAIL, "123456", SENSITIVE_PASSWORD))));

        List<String> logLines = collectLogLines();
        assertThat(logLines).isNotEmpty();

        String joined = String.join("\n", logLines);

        assertThat(joined)
                .as("senha em texto puro nunca deve aparecer nos logs")
                .doesNotContain(SENSITIVE_PASSWORD);

        assertThat(joined)
                .as("token de registro (JWT) nunca deve aparecer nos logs")
                .doesNotContain(registrationToken);

        assertThat(joined)
                .as("e-mail completo nunca deve aparecer nos logs (apenas mascarado)")
                .doesNotContain(SENSITIVE_EMAIL);

        assertThat(joined)
                .as("parte local do e-mail nunca deve aparecer integralmente nos logs")
                .doesNotContain(SENSITIVE_LOCAL_PART);

        assertThat(joined)
                .as("nickname sensivel nunca deve aparecer nos logs")
                .doesNotContain(SENSITIVE_NICKNAME);
    }

    private List<String> collectLogLines() {
        List<String> lines = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            lines.add(event.getFormattedMessage());
            IThrowableProxy throwable = event.getThrowableProxy();
            while (throwable != null) {
                lines.add(throwable.getClassName() + ": " + throwable.getMessage());
                throwable = throwable.getCause();
            }
        }
        return lines;
    }
}
