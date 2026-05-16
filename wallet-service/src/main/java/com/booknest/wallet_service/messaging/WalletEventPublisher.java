package com.booknest.wallet_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.booknest.wallet_service.config.RabbitConfig;
import com.booknest.wallet_service.dto.WalletEvent;

@Service
public class WalletEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(WalletEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public WalletEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishWalletEvent(Long userId, double amount, String eventType, String message) {
        try {
            WalletEvent event = new WalletEvent(userId, amount, eventType, message);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_WALLET, event);
            logger.info("Published {} event for user ID: {}", eventType, userId);
        } catch (Exception e) {
            logger.error("Failed to publish {} event for user ID: {}", eventType, userId, e);
        }
    }
}
