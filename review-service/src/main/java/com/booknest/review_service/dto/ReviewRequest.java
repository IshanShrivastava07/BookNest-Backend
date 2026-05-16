package com.booknest.review_service.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long bookId;
    private Long userId;
    private int rating;
    private String comment;
}