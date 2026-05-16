package com.booknest.wishlist_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    private Long wishlistId;
    private Long bookId;
    private String bookTitle;
    private double price;
}