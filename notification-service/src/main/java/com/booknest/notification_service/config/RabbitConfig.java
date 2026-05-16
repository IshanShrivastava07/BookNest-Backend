package com.booknest.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public DirectExchange orderExchange(@Value("${booknest.rabbitmq.order-exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationQueue(@Value("${booknest.rabbitmq.notification-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue authQueue(@Value("${booknest.rabbitmq.auth-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue walletQueue(@Value("${booknest.rabbitmq.wallet-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding orderNotificationBinding(
            Queue notificationQueue,
            DirectExchange orderExchange,
            @Value("${booknest.rabbitmq.routing-key}") String routingKey) {
        return BindingBuilder.bind(notificationQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    public Binding authNotificationBinding(
            Queue authQueue,
            DirectExchange orderExchange,
            @Value("${booknest.rabbitmq.auth-routing-key}") String routingKey) {
        return BindingBuilder.bind(authQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    public Binding walletNotificationBinding(
            Queue walletQueue,
            DirectExchange orderExchange,
            @Value("${booknest.rabbitmq.wallet-routing-key}") String routingKey) {
        return BindingBuilder.bind(walletQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitListenerErrorHandler rabbitListenerErrorHandler() {
        return (amqpMessage, message, exception) -> {
            // Log the error and drop the message to prevent infinite poison pill loops
            System.err.println("Error processing message: " + new String(amqpMessage.getBody()) + ". Exception: " + exception.getMessage());
            return null; // Return null to indicate the message is handled and should be acked/dropped
        };
    }
}
