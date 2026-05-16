package com.booknest.book_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.book_service.entity.Book;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByGenreIgnoreCase(String genre);
}