package com.booknest.review_service.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderPurchaseClient {

    @GetMapping("/orders/verify-purchase/{bookId}")
    Map<String, Boolean> verifyPurchase(@PathVariable("bookId") Long bookId);
}
