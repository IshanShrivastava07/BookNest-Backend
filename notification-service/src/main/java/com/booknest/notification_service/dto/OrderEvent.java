package com.booknest.notification_service.dto;

import lombok.Data;

@Data
public class OrderEvent {
    private Long userId;
    private String eventType;
    private String message;
}
