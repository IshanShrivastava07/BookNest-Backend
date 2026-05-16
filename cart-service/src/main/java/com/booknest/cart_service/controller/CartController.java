package com.booknest.cart_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.booknest.cart_service.dto.CartRequest;
import com.booknest.cart_service.entity.Cart;
import com.booknest.cart_service.entity.CartItem;
import com.booknest.cart_service.service.CartService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private HttpServletRequest request;

    private Long getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("SOURCE: CART-SERVICE | Missing X-User-Id header");
        }
        return Long.parseLong(userIdStr);
    }

    @GetMapping
    public List<CartItem> getCart() {
        return cartService.getCartItems(getCurrentUserId());
    }

    /** Preferred: add by book id (quantity optional, default 1) */
    @PostMapping("/add/{bookId}")
    public ResponseEntity<?> addByBookId(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") int quantity) {
        try {
            CartRequest cartRequest = new CartRequest();
            cartRequest.setUserId(getCurrentUserId());
            cartRequest.setBookId(bookId);
            cartRequest.setQuantity(quantity);
            Cart cart = cartService.addItem(cartRequest);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while adding item to cart: " + e.getMessage());
        }
    }

    /** Legacy body-based add (still supported) */
    @PostMapping
    public ResponseEntity<?> addItem(@RequestBody CartRequest cartRequest) {
        try {
            cartRequest.setUserId(getCurrentUserId());
            Cart cart = cartService.addItem(cartRequest);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while adding item to cart: " + e.getMessage());
        }
    }

    @PutMapping("/update/{bookId}")
    public ResponseEntity<?> updateByBookId(@PathVariable Long bookId, @RequestParam int quantity) {
        try {
            CartItem item = cartService.updateQuantityByBookId(getCurrentUserId(), bookId, quantity);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Remove line matching bookId (fixes client/server id confusion with itemId) */
    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<?> removeByBookId(@PathVariable Long bookId) {
        try {
            cartService.removeByBookId(getCurrentUserId(), bookId);
            return ResponseEntity.ok("Item removed from cart");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long itemId, @RequestParam int quantity) {
        try {
            CartItem item = cartService.updateQuantity(itemId, getCurrentUserId(), quantity);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable Long itemId) {
        try {
            cartService.removeItem(itemId, getCurrentUserId());
            return ResponseEntity.ok("Item removed from cart");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart() {
        try {
            cartService.clearCart(getCurrentUserId());
            return ResponseEntity.ok("Cart cleared");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
