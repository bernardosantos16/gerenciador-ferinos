package com.bernardo.geradortimes.user.rabbitmq.email_verification;

import com.bernardo.geradortimes.shared.observability.LogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailVerificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:user.events}")
    private String exchange;

    @Value("${app.rabbitmq.email-verification.routing-key:user.email-verification}")
    private String routingKey;

    public EmailVerificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(EmailVerificationEvent event) {
        log.info("Publicando evento de verificacao de email - email: {}", LogSanitizer.maskEmail(event.email()));
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Evento de verificacao de email publicado com sucesso - email: {}", LogSanitizer.maskEmail(event.email()));
    }
}
