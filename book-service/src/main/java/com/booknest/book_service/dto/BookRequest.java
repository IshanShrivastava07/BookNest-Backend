package com.booknest.book_service.dto;

import lombok.Data;

@Data
public class BookRequest {
    private String title;
    private String author;
    private String genre;
    private double price;
    private int stock;
    private String description;
    private String coverImage;
}