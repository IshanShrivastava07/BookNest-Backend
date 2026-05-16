package com.booknest.cart_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.cart_service.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByUserId(Long userId);
}
