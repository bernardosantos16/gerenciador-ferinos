package com.bernardo.geradortimes.user.rabbitmq.password_reset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PasswordResetProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:user.events}")
    private String exchange;

    @Value("${app.rabbitmq.password-reset.routing-key:user.password-reset}")
    private String routingKey;

    public PasswordResetProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(PasswordResetEvent event) {
        log.info("Publicando evento de recuperacao de senha - userId: {}", event.userId());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Evento de recuperacao de senha publicado com sucesso - userId: {}", event.userId());
    }
}
