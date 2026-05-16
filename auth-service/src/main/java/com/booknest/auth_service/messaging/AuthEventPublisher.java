package com.booknest.auth_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.booknest.auth_service.config.RabbitConfig;
import com.booknest.auth_service.dto.AuthEvent;

@Service
public class AuthEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AuthEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public AuthEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(Long userId, String email, String fullName) {
        try {
            AuthEvent event = new AuthEvent(userId, email, fullName, "USER_REGISTERED", "Welcome to BookNest, " + fullName + "!");
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_AUTH, event);
            logger.info("Published USER_REGISTERED event for user ID: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to publish USER_REGISTERED event for user ID: {}", userId, e);
            // We swallow the exception here to ensure it doesn't break the auth flow.
        }
    }
}
