package com.booknest.notification_service.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.booknest.notification_service.dto.AuthEvent;
import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.service.NotificationService;

@Component
@Slf4j
public class AuthEventListener {
    private final NotificationService notificationService;

    public AuthEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${booknest.rabbitmq.auth-queue}", errorHandler = "rabbitListenerErrorHandler")
    public void onAuthEvent(AuthEvent event) {
        try {
            if (event == null || event.getUserId() == null) {
                return;
            }
            String type = event.getEventType() != null ? event.getEventType() : "AUTH_EVENT";
            String message = event.getMessage() != null ? event.getMessage() : type;
            NotificationRequest request = new NotificationRequest();
            request.setUserId(event.getUserId());
            request.setMessage("[" + type + "] " + message);
            notificationService.send(request);
            log.info("Successfully processed auth event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process auth event. Safe retry/drop handled.", e);
            throw e; // Let spring amqp handle retry or DLQ
        }
    }
}
