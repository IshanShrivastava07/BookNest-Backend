package com.booknest.order_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.booknest.order_service.dto.OrderEvent;

@Component
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public OrderEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${booknest.rabbitmq.order-exchange}") String exchange,
            @Value("${booknest.rabbitmq.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(OrderEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
