package com.booknest.book_service.service;

import com.booknest.book_service.dto.BookRequest;
import com.booknest.book_service.entity.Book;
import com.booknest.book_service.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository repo;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookRequest bookRequest;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setBookId(1L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setGenre("Technology");
        book.setPrice(350.0);
        book.setStock(10);

        bookRequest = new BookRequest();
        bookRequest.setTitle("Clean Code");
        bookRequest.setAuthor("Robert Martin");
        bookRequest.setGenre("Technology");
        bookRequest.setPrice(350.0);
        bookRequest.setStock(10);
        bookRequest.setDescription("A handbook of agile software craftsmanship");
        bookRequest.setCoverImage("cover.jpg");
    }

    // ─── addBook ─────────────────────────────────────────────────────────────

    @Test
    void addBook_ShouldSaveAndReturn() {
        when(repo.save(any(Book.class))).thenReturn(book);

        Book result = bookService.addBook(bookRequest);

        assertNotNull(result);
        assertEquals("Clean Code", result.getTitle());
        verify(repo).save(any(Book.class));
    }

    // ─── updateBook ──────────────────────────────────────────────────────────

    @Test
    void updateBook_Existing_ShouldUpdateAndReturn() {
        when(repo.findById(1L)).thenReturn(Optional.of(book));
        when(repo.save(any(Book.class))).thenReturn(book);

        Book result = bookService.updateBook(1L, bookRequest);

        assertEquals("Clean Code", result.getTitle());
        verify(repo).save(book);
    }

    @Test
    void updateBook_NotFound_ShouldThrow() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookService.updateBook(99L, bookRequest));
    }

    // ─── deleteBook ──────────────────────────────────────────────────────────

    @Test
    void deleteBook_Existing_ShouldDelete() {
        when(repo.existsById(1L)).thenReturn(true);

        bookService.deleteBook(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void deleteBook_NotFound_ShouldThrow() {
        when(repo.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> bookService.deleteBook(99L));
        verify(repo, never()).deleteById(any());
    }

    // ─── getAllBooks ──────────────────────────────────────────────────────────

    @Test
    void getAllBooks_ShouldReturnList() {
        when(repo.findAll()).thenReturn(List.of(book));

        List<Book> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        verify(repo).findAll();
    }

    // ─── searchByTitle ───────────────────────────────────────────────────────

    @Test
    void searchByTitle_ShouldReturnMatchingBooks() {
        when(repo.findByTitleContainingIgnoreCase("clean")).thenReturn(List.of(book));

        List<Book> result = bookService.searchByTitle("clean");

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).getTitle());
    }

    // ─── getBookById ─────────────────────────────────────────────────────────

    @Test
    void getBookById_Existing_ShouldReturn() {
        when(repo.findById(1L)).thenReturn(Optional.of(book));

        Book result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getBookId());
    }

    @Test
    void getBookById_NotFound_ShouldReturnNull() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        Book result = bookService.getBookById(99L);

        assertNull(result);
    }
}
