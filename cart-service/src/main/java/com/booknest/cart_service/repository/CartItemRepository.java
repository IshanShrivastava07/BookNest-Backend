package com.booknest.cart_service.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.cart_service.entity.CartItem;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);
    void deleteByCartId(Long cartId);
}