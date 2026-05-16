package com.booknest.order_service.dto;
import lombok.Data;

@Data
public class NotificationRequest {

    private Long userId;
    private String message;
}
