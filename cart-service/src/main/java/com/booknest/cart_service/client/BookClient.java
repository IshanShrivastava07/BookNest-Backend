package com.booknest.cart_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.booknest.cart_service.dto.Book;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{id}")
    Book getBookById(@PathVariable("id") Long id);
}
