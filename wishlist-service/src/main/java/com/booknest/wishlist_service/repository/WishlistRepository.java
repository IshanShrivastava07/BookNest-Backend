package com.booknest.wishlist_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.wishlist_service.entity.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Wishlist findByUserId(Long userId);
}
