package com.booknest.cart_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    private Long cartId;
    private Long bookId;
    private String bookTitle;
    private String author;
    private String coverImage;
    private double price;
    private int quantity;
}