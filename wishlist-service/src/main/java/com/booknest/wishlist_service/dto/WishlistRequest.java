package com.booknest.wishlist_service.dto;

import lombok.Data;

@Data
public class WishlistRequest {
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private double price;
}