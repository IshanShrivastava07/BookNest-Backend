package com.booknest.cart_service.service;

import java.util.List;

import com.booknest.cart_service.dto.CartRequest;
import com.booknest.cart_service.entity.Cart;
import com.booknest.cart_service.entity.CartItem;

public interface CartService {
    Cart addItem(CartRequest request);
    List<CartItem> getCartItems(Long userId);
    CartItem updateQuantity(Long itemId, Long userId, int quantity);
    CartItem updateQuantityByBookId(Long userId, Long bookId, int quantity);
    void removeItem(Long itemId, Long userId);
    void removeByBookId(Long userId, Long bookId);
    void clearCart(Long userId);
}
