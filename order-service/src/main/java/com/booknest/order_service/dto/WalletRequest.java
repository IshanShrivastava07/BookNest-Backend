package com.booknest.order_service.dto;


import lombok.Data;

@Data
public class WalletRequest {

    private Long userId;
    private double amount;
}