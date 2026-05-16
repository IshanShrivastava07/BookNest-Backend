package com.booknest.book_service.service;

import java.util.List;
import com.booknest.book_service.dto.BookRequest;
import com.booknest.book_service.entity.Book;

public interface BookService {
    Book addBook(BookRequest request);
    Book updateBook(Long id, BookRequest request);
    void deleteBook(Long id);
    List<Book> getAllBooks();
    List<Book> searchByTitle(String title);
    Book getBookById(Long id);
}