package com.booknest.wallet_service.dto;

import lombok.Data;

@Data
public class WalletRequest {
    private Long userId;
    private double amount;
    /** Optional; used for verified top-up credits. */
    private String description;
}
