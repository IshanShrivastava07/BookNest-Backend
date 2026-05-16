package com.booknest.order_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.booknest.order_service.dto.CartItem;

/**
 * Cart APIs are user-scoped via {@code X-User-Id} (propagated from gateway by {@link FeignConfig}).
 * Previous paths {@code /cart/{userId}} did not exist and caused Feign 404 → order placement failures.
 */
@FeignClient(name = "cart-service")
public interface CartClient {

    @GetMapping("/cart")
    List<CartItem> getCart();

    @DeleteMapping("/cart/clear")
    void clearCart();
}
