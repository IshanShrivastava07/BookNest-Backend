package com.booknest.review_service.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booknest.review_service.client.OrderPurchaseClient;
import com.booknest.review_service.dto.ReviewBookResponse;
import com.booknest.review_service.dto.ReviewRequest;
import com.booknest.review_service.entity.Review;
import com.booknest.review_service.repository.ReviewRepository;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository repo;

    @Autowired
    private OrderPurchaseClient orderPurchaseClient;

    private void assertPurchased(Long userId, Long bookId) {
        Map<String, Boolean> res = orderPurchaseClient.verifyPurchase(bookId);
        Boolean ok = res != null ? res.get("purchased") : null;
        if (ok == null || !ok) {
            throw new IllegalStateException("Only verified purchasers can review this book");
        }
    }

    @Override
    @Transactional
    public Review addReview(Long userId, ReviewRequest request) {
        if (request == null || request.getBookId() == null) {
            throw new IllegalArgumentException("Request and Book ID are required");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        assertPurchased(userId, request.getBookId());

        Review r = new Review();
        r.setBookId(request.getBookId());
        r.setUserId(userId);
        r.setRating(request.getRating());
        r.setComment(request.getComment());
        r.setReviewDate(LocalDateTime.now());

        return repo.save(r);
    }

    @Override
    public ReviewBookResponse getReviewsForBook(Long bookId) {
        Double avg = repo.getAverageRating(bookId);
        if (avg != null) {
            avg = Math.round(avg * 10.0) / 10.0;
        }
        return new ReviewBookResponse(repo.findByBookId(bookId), avg);
    }

    @Override
    @Transactional
    public Review updateReview(Long userId, String role, Long reviewId, ReviewRequest request) {
        Review r = repo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        if (!r.getUserId().equals(userId) && !"ROLE_ADMIN".equals(role)) {
            throw new IllegalStateException("Not allowed to edit this review");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        r.setRating(request.getRating());
        r.setComment(request.getComment());
        r.setReviewDate(LocalDateTime.now());
        return repo.save(r);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, String role, Long reviewId) {
        Review r = repo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        if (!r.getUserId().equals(userId) && !"ROLE_ADMIN".equals(role)) {
            throw new IllegalStateException("Not allowed to delete this review");
        }
        repo.delete(r);
    }
}
