package com.bernardo.geradortimes.user.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:user.events}")
    private String exchange;

    @Value("${app.rabbitmq.email-verification.queue:user.email.verification.queue}")
    private String emailVerificationQueue;

    @Value("${app.rabbitmq.email-verification.routing-key:user.email-verification}")
    private String emailVerificationRoutingKey;

    @Value("${app.rabbitmq.password-reset.queue:user.email.password-reset.queue}")
    private String passwordResetQueue;

    @Value("${app.rabbitmq.password-reset.routing-key:user.password-reset}")
    private String passwordResetRoutingKey;

    @Value("${app.rabbitmq.club.exchange:club.events}")
    private String clubExchangeName;

    @Value("${app.rabbitmq.club.membership-request.queue:club.membership.request.queue}")
    private String clubMembershipRequestQueueName;

    @Value("${app.rabbitmq.club.membership-request.routing-key:club.membership-requested}")
    private String clubMembershipRequestRoutingKey;

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue userEmailPasswordResetQueue() {
        return new Queue(passwordResetQueue, true);
    }

    @Bean
    public Binding passwordResetBinding(Queue userEmailPasswordResetQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userEmailPasswordResetQueue).to(userExchange).with(passwordResetRoutingKey);
    }

    @Bean
    public Queue userEmailVerificationQueue() {
        return new Queue(emailVerificationQueue, true);
    }

    @Bean
    public Binding emailVerificationBinding(Queue userEmailVerificationQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userEmailVerificationQueue).to(userExchange).with(emailVerificationRoutingKey);
    }

    @Bean
    public TopicExchange clubExchange() {
        return new TopicExchange(clubExchangeName);
    }

    @Bean
    public Queue clubMembershipRequestQueue() {
        return new Queue(clubMembershipRequestQueueName, true);
    }

    @Bean
    public Binding clubMembershipRequestBinding(Queue clubMembershipRequestQueue, TopicExchange clubExchange) {
        return BindingBuilder.bind(clubMembershipRequestQueue).to(clubExchange).with(clubMembershipRequestRoutingKey);
    }
}
