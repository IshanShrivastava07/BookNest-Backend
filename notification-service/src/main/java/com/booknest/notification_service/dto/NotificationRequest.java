package com.booknest.notification_service.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long userId;
    private String message;
}