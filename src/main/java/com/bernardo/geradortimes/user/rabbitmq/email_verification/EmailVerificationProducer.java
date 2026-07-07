package com.bernardo.geradortimes.user.rabbitmq.email_verification;

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
        log.info("Publicando evento de verificacao de email - email: {}", event.email());
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Evento de verificacao de email publicado com sucesso - email: {}", event.email());
        } catch (Exception e) {
            log.error("Erro ao publicar evento de verificacao de email - email: {}", event.email(), e);
        }
    }
}
