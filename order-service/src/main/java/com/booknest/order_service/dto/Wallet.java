package com.booknest.order_service.dto;
import lombok.Data;

@Data
public class Wallet {

    private Long walletId;
    private Long userId;
    private double balance;
}
