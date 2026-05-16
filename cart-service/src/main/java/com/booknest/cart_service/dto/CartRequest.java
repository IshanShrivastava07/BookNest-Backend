package com.booknest.cart_service.dto;

import lombok.Data;

@Data
public class CartRequest {
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private double price;
    private int quantity;
}
