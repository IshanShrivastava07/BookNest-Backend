package com.booknest.book_service.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.booknest.book_service.dto.BookRequest;
import com.booknest.book_service.entity.Book;
import com.booknest.book_service.repository.BookRepository;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository repo;

    @Override
    @CacheEvict(value = "books", allEntries = true)
    public Book addBook(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setGenre(request.getGenre());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setDescription(request.getDescription());
        book.setCoverImage(request.getCoverImage());

        Book saved = repo.save(book);
        log.info("Book added: title={}, id={}", saved.getTitle(), saved.getBookId());
        return saved;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "book", key = "#id"),
        @CacheEvict(value = "books", allEntries = true)
    })
    public Book updateBook(Long id, BookRequest request) {
        Book book = repo.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setGenre(request.getGenre());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setDescription(request.getDescription());
        book.setCoverImage(request.getCoverImage());
        Book saved = repo.save(book);
        log.info("Book updated: id={}, title={}", id, saved.getTitle());
        return saved;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "book", key = "#id"),
        @CacheEvict(value = "books", allEntries = true)
    })
    public void deleteBook(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Book not found");
        }
        repo.deleteById(id);
        log.info("Book deleted: id={}", id);
    }

    @Override
    @Cacheable(value = "books")
    public List<Book> getAllBooks() {
        return repo.findAll();
    }

    @Override
    public List<Book> searchByTitle(String title) {
        return repo.findByTitleContainingIgnoreCase(title);
    }

    @Override
    @Cacheable(value = "book", key = "#id")
    public Book getBookById(Long id) {
        return repo.findById(id).orElse(null);
    }
}