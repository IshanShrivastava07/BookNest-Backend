package com.booknest.wishlist_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.booknest.wishlist_service.entity.WishlistItem;
import com.booknest.wishlist_service.service.WishlistService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService service;

    @Autowired
    private HttpServletRequest request;

    private Long getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("SOURCE: WISHLIST-SERVICE | Missing X-User-Id header");
        }
        return Long.parseLong(userIdStr);
    }

    @GetMapping
    public List<WishlistItem> getWishlist() {
        return service.getWishlist(getCurrentUserId());
    }

    @PostMapping("/add/{bookId}")
    public ResponseEntity<?> add(@PathVariable Long bookId) {
        try {
            return ResponseEntity.ok(service.addBook(getCurrentUserId(), bookId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<?> remove(@PathVariable Long bookId) {
        service.removeByBookId(getCurrentUserId(), bookId);
        return ResponseEntity.ok("Removed");
    }

    @PostMapping("/move-to-cart/{bookId}")
    public ResponseEntity<?> moveToCart(@PathVariable Long bookId) {
        try {
            service.moveToCart(getCurrentUserId(), bookId);
            return ResponseEntity.ok("Moved to cart");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
