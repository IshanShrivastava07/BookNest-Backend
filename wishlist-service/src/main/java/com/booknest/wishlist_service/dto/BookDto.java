package com.booknest.wishlist_service.dto;

import lombok.Data;

@Data
public class BookDto {
    private Long bookId;
    private String title;
    private String author;
    private double price;
    private String coverImage;
}
