package com.booknest.wallet_service.dto;

import lombok.Data;

@Data
public class VerifyTopupRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
