package com.booknest.wishlist_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.wishlist_service.entity.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByWishlistId(Long wishlistId);

    Optional<WishlistItem> findByWishlistIdAndBookId(Long wishlistId, Long bookId);

    void deleteByWishlistIdAndBookId(Long wishlistId, Long bookId);
}
