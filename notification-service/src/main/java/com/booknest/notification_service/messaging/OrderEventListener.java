package com.booknest.notification_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.dto.OrderEvent;
import com.booknest.notification_service.service.NotificationService;

@Component
@Slf4j
public class OrderEventListener {
    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${booknest.rabbitmq.notification-queue}")
    public void onOrderEvent(OrderEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }
        String type = event.getEventType() != null ? event.getEventType() : "ORDER_EVENT";
        String message = event.getMessage() != null ? event.getMessage() : type;
        NotificationRequest request = new NotificationRequest();
        request.setUserId(event.getUserId());
        request.setMessage("[" + type + "] " + message);
        log.info("Received order event: type={}, userId={}", type, event.getUserId());
        notificationService.send(request);
        log.info("Notification dispatched for userId={}", event.getUserId());
    }
}
