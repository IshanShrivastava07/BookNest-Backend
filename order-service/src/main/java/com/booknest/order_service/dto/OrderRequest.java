package com.booknest.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderRequest {

    private Long userId;
    private double amount;
    private String paymentMode;
    private int quantity;

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[A-Za-z ]{2,50}$", message = "Name must contain only alphabets and spaces (2-50 characters)")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String mobile;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Invalid Pincode (must be 6 digits)")
    private String pincode;

    /** Required when paymentMode is RAZORPAY — verified server-side before persisting order */
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
