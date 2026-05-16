package com.booknest.review_service.service;

import com.booknest.review_service.client.OrderPurchaseClient;
import com.booknest.review_service.dto.ReviewRequest;
import com.booknest.review_service.entity.Review;
import com.booknest.review_service.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository repo;
    @Mock private OrderPurchaseClient orderPurchaseClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review review;
    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        request = new ReviewRequest();
        request.setBookId(1L);
        request.setRating(4);
        request.setComment("Great book!");

        review = new Review();
        review.setBookId(1L);
        review.setUserId(10L);
        review.setRating(4);
        review.setComment("Great book!");
    }

    // ─── addReview ────────────────────────────────────────────────────────────

    @Test
    void addReview_InvalidRatingTooLow_ShouldThrow() {
        request.setRating(0);

        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(10L, request));
    }

    @Test
    void addReview_InvalidRatingTooHigh_ShouldThrow() {
        request.setRating(6);

        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(10L, request));
    }

    @Test
    void addReview_UserNotPurchased_ShouldThrow() {
        when(orderPurchaseClient.verifyPurchase(1L)).thenReturn(Map.of("purchased", false));

        assertThrows(IllegalStateException.class, () -> reviewService.addReview(10L, request));
    }

    @Test
    void addReview_ValidPurchased_ShouldSave() {
        when(orderPurchaseClient.verifyPurchase(1L)).thenReturn(Map.of("purchased", true));
        when(repo.save(any(Review.class))).thenReturn(review);

        Review result = reviewService.addReview(10L, request);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        verify(repo).save(any(Review.class));
    }

    // ─── updateReview ─────────────────────────────────────────────────────────

    @Test
    void updateReview_NotOwnerNotAdmin_ShouldThrow() {
        review.setUserId(99L); // different user
        when(repo.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(IllegalStateException.class,
                () -> reviewService.updateReview(10L, "ROLE_USER", 1L, request));
    }

    @Test
    void updateReview_AdminCanUpdate_ShouldSucceed() {
        review.setUserId(99L); // different user but admin
        when(repo.findById(1L)).thenReturn(Optional.of(review));
        when(repo.save(any(Review.class))).thenReturn(review);

        Review result = reviewService.updateReview(10L, "ROLE_ADMIN", 1L, request);

        assertEquals(4, result.getRating());
    }

    @Test
    void updateReview_OwnerUpdate_ShouldSucceed() {
        when(repo.findById(1L)).thenReturn(Optional.of(review));
        when(repo.save(any(Review.class))).thenReturn(review);

        Review result = reviewService.updateReview(10L, "ROLE_USER", 1L, request);

        assertEquals("Great book!", result.getComment());
    }

    @Test
    void updateReview_InvalidRating_ShouldThrow() {
        request.setRating(10);
        when(repo.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(IllegalArgumentException.class,
                () -> reviewService.updateReview(10L, "ROLE_USER", 1L, request));
    }

    // ─── deleteReview ─────────────────────────────────────────────────────────

    @Test
    void deleteReview_NotOwnerNotAdmin_ShouldThrow() {
        review.setUserId(99L);
        when(repo.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(IllegalStateException.class,
                () -> reviewService.deleteReview(10L, "ROLE_USER", 1L));
    }

    @Test
    void deleteReview_AdminCanDelete_ShouldSucceed() {
        review.setUserId(99L);
        when(repo.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(10L, "ROLE_ADMIN", 1L);

        verify(repo).delete(review);
    }

    @Test
    void deleteReview_OwnerDelete_ShouldSucceed() {
        when(repo.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(10L, "ROLE_USER", 1L);

        verify(repo).delete(review);
    }

    @Test
    void deleteReview_NotFound_ShouldThrow() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> reviewService.deleteReview(10L, "ROLE_USER", 99L));
    }

    @Test
    void addReview_NullInputs_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(10L, null));
        request.setBookId(null);
        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(10L, request));
    }

    @Test
    void updateReview_NotFound_ShouldThrow() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> reviewService.updateReview(10L, "ROLE_USER", 99L, request));
    }

    @Test
    void getReviewsForBook_Empty_ShouldReturnZeroAvg() {
        when(repo.findByBookId(1L)).thenReturn(java.util.Collections.emptyList());
        var res = reviewService.getReviewsForBook(1L);
        assertEquals(0.0, res.getAverageRating());
        assertEquals(0, res.getReviews().size());
    }

    @Test
    void getReviewsForBook_WithReviews_ShouldCalculateAvg() {
        Review r1 = new Review(); r1.setRating(5);
        Review r2 = new Review(); r2.setRating(3);
        when(repo.findByBookId(1L)).thenReturn(java.util.List.of(r1, r2));
        when(repo.getAverageRating(1L)).thenReturn(4.0);
        
        var res = reviewService.getReviewsForBook(1L);
        assertEquals(4.0, res.getAverageRating());
        assertEquals(2, res.getReviews().size());
    }
}
