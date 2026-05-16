package com.booknest.order_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentOrderResponse {
    private String orderId;
    private int amount;
    private String currency;
    private String key; // send key to frontend
}
