package com.booknest.wishlist_service.service;

import java.util.List;

import com.booknest.wishlist_service.entity.WishlistItem;

public interface WishlistService {
    WishlistItem addBook(Long userId, Long bookId);
    List<WishlistItem> getWishlist(Long userId);
    void removeByBookId(Long userId, Long bookId);
    void moveToCart(Long userId, Long bookId);
}
