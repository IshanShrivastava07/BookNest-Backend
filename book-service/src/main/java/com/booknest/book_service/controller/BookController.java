package com.booknest.book_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.booknest.book_service.dto.BookRequest;
import com.booknest.book_service.entity.Book;
import com.booknest.book_service.service.BookService;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    @Autowired
    private BookService service;

    @PostMapping
    public Book addBook(@RequestBody BookRequest request) {
        return service.addBook(request);
    }

    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody BookRequest request) {
        return service.updateBook(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable Long id) {
        service.deleteBook(id);
        return "Book deleted";
    }

    @GetMapping
    public List<Book> getAll() {
        return service.getAllBooks();
    }

    @GetMapping("/search")
    public List<Book> search(@RequestParam String title) {
        return service.searchByTitle(title);
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return service.getBookById(id);
    }

    // Admin Endpoints
    @PostMapping("/admin/create")
    public Book addBookAdmin(@RequestBody BookRequest request) {
        return service.addBook(request);
    }

    @PutMapping("/admin/update/{id}")
    public Book updateBookAdmin(@PathVariable Long id, @RequestBody BookRequest request) {
        return service.updateBook(id, request);
    }

    @DeleteMapping("/admin/delete/{id}")
    public String deleteBookAdmin(@PathVariable Long id) {
        service.deleteBook(id);
        return "Book deleted";
    }
}
