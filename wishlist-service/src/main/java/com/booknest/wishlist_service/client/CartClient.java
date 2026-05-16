package com.booknest.wishlist_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cart-service")
public interface CartClient {

    @PostMapping("/cart/add/{bookId}")
    String addToCart(@PathVariable("bookId") Long bookId, @RequestParam(value = "quantity", defaultValue = "1") int quantity);
}
