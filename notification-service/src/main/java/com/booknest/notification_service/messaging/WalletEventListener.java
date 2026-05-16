package com.booknest.notification_service.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.dto.WalletEvent;
import com.booknest.notification_service.service.NotificationService;

@Component
@Slf4j
public class WalletEventListener {
    private final NotificationService notificationService;

    public WalletEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${booknest.rabbitmq.wallet-queue}", errorHandler = "rabbitListenerErrorHandler")
    public void onWalletEvent(WalletEvent event) {
        try {
            if (event == null || event.getUserId() == null) {
                return;
            }
            String type = event.getEventType() != null ? event.getEventType() : "WALLET_EVENT";
            String message = event.getMessage() != null ? event.getMessage() : type;
            NotificationRequest request = new NotificationRequest();
            request.setUserId(event.getUserId());
            request.setMessage("[" + type + "] " + message);
            notificationService.send(request);
            log.info("Successfully processed wallet event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process wallet event. Safe retry/drop handled.", e);
            throw e;
        }
    }
}
