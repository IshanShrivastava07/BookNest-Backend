package com.booknest.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletEvent {
    private Long userId;
    private double amount;
    private String eventType;
    private String message;
}
