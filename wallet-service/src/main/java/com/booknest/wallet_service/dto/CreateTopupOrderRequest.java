package com.booknest.wallet_service.dto;

import lombok.Data;

@Data
public class CreateTopupOrderRequest {
    /** Amount in INR. */
    private Double amount;
}
