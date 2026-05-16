package com.booknest.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopupOrderResponse {
    private String orderId;
    private double amount;
    private String key;
}
