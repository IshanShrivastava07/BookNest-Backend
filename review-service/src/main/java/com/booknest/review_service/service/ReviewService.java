package com.booknest.review_service.service;

import com.booknest.review_service.dto.ReviewBookResponse;
import com.booknest.review_service.dto.ReviewRequest;
import com.booknest.review_service.entity.Review;

public interface ReviewService {
    Review addReview(Long userId, ReviewRequest request);
    ReviewBookResponse getReviewsForBook(Long bookId);
    Review updateReview(Long userId, String role, Long reviewId, ReviewRequest request);
    void deleteReview(Long userId, String role, Long reviewId);
}
