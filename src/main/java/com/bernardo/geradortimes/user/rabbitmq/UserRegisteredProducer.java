package com.bernardo.geradortimes.user.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserRegisteredProducer {

     final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:user.events}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:user.registered}")
    private String routingKey;

    public UserRegisteredProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(UserRegisteredEvent event) {
        log.info("Publicando evento de registro de usuario - userId: {}", event.userId());
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Evento de registro de usuario publicado com sucesso - userId: {}", event.userId());
        } catch (Exception e) {
            log.error("Erro ao publicar evento de registro de usuario - userId: {}", event.userId(), e);
        }
    }
}
