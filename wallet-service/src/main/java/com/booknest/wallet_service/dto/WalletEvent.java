package com.booknest.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletEvent {
    private Long userId;
    private double amount;
    private String eventType; // WALLET_TOPUP_SUCCESS, PAYMENT_SUCCESS
    private String message;
}
