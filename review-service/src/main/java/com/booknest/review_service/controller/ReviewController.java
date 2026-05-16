package com.booknest.review_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.booknest.review_service.dto.ReviewBookResponse;
import com.booknest.review_service.dto.ReviewRequest;
import com.booknest.review_service.entity.Review;
import com.booknest.review_service.service.ReviewService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService service;

    @Autowired
    private HttpServletRequest request;

    private Long getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("SOURCE: REVIEW-SERVICE | Missing X-User-Id header");
        }
        return Long.parseLong(userIdStr);
    }

    private String getCurrentUserRole() {
        return request.getHeader("X-User-Role");
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody ReviewRequest body) {
        try {
            Review r = service.addReview(getCurrentUserId(), body);
            return ResponseEntity.ok(r);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/book/{bookId}")
    public ReviewBookResponse getForBook(@PathVariable Long bookId) {
        return service.getReviewsForBook(bookId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ReviewRequest body) {
        try {
            return ResponseEntity.ok(service.updateReview(getCurrentUserId(), getCurrentUserRole(), id, body));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteReview(getCurrentUserId(), getCurrentUserRole(), id);
            return ResponseEntity.ok("Deleted");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
