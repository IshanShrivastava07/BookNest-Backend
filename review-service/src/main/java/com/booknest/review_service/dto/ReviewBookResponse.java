package com.booknest.review_service.dto;

import java.util.List;

import com.booknest.review_service.entity.Review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBookResponse {
    private List<Review> reviews;
    private Double averageRating;
}
