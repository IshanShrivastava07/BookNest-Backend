package com.booknest.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.booknest.order_service.dto.Notification;
import com.booknest.order_service.dto.NotificationRequest;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications")
    Notification send(@RequestBody NotificationRequest request);
}