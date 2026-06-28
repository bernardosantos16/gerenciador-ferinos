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

    @Value("${app.rabbitmq.queue:user.email.confirmation.queue}")
    private String confirmationQueue;

    @Value("${app.rabbitmq.routing-key:user.registered}")
    private String confirmationRoutingKey;

    @Value("${app.rabbitmq.password-reset.queue:user.email.password-reset.queue}")
    private String passwordResetQueue;

    @Value("${app.rabbitmq.password-reset.routing-key:user.password-reset}")
    private String passwordResetRoutingKey;

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
    public Queue userEmailConfirmationQueue() {
        return new Queue(confirmationQueue, true);
    }

    @Bean
    public Binding confirmationBinding(Queue userEmailConfirmationQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userEmailConfirmationQueue).to(userExchange).with(confirmationRoutingKey);
    }

    @Bean
    public Queue userEmailPasswordResetQueue() {
        return new Queue(passwordResetQueue, true);
    }

    @Bean
    public Binding passwordResetBinding(Queue userEmailPasswordResetQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userEmailPasswordResetQueue).to(userExchange).with(passwordResetRoutingKey);
    }
}
