package com.booknest.wishlist_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.booknest.wishlist_service.dto.BookDto;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{id}")
    BookDto getBookById(@PathVariable("id") Long id);
}
