package com.booknest.order_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {

    private Long notificationId;
    private Long userId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
}
