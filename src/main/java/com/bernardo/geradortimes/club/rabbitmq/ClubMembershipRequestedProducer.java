package com.bernardo.geradortimes.club.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClubMembershipRequestedProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.club.exchange:club.events}")
    private String exchange;

    @Value("${app.rabbitmq.club.membership-request.routing-key:club.membership-requested}")
    private String routingKey;

    public ClubMembershipRequestedProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ClubMembershipRequestedEvent event) {
        log.info("Publicando evento de solicitacao de ingresso - clubId: {}, directorUserId: {}",
                event.clubId(), event.directorUserId());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Evento de solicitacao de ingresso publicado - clubId: {}", event.clubId());
    }
}
