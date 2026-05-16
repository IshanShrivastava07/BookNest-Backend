package com.booknest.book_service.controller;

import com.booknest.book_service.dto.BookRequest;
import com.booknest.book_service.entity.Book;
import com.booknest.book_service.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService service;

    @InjectMocks
    private BookController controller;

    private MockMvc mockMvc;

    private Book book;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        book = new Book();
        book.setBookId(1L);
        book.setTitle("Test Book");
    }

    @Test
    void addBook() throws Exception {
        when(service.addBook(any())).thenReturn(book);
        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Book\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void updateBook() throws Exception {
        when(service.updateBook(eq(1L), any())).thenReturn(book);
        mockMvc.perform(put("/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Book\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void deleteBook() throws Exception {
        mockMvc.perform(delete("/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Book deleted"));
        verify(service).deleteBook(1L);
    }

    @Test
    void getAll() throws Exception {
        when(service.getAllBooks()).thenReturn(List.of(book));
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    @Test
    void search() throws Exception {
        when(service.searchByTitle("Test")).thenReturn(List.of(book));
        mockMvc.perform(get("/books/search").param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    @Test
    void getBookById() throws Exception {
        when(service.getBookById(1L)).thenReturn(book);
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void addBookAdmin() throws Exception {
        when(service.addBook(any())).thenReturn(book);
        mockMvc.perform(post("/books/admin/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Book\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void updateBookAdmin() throws Exception {
        when(service.updateBook(eq(1L), any())).thenReturn(book);
        mockMvc.perform(put("/books/admin/update/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Book\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void deleteBookAdmin() throws Exception {
        mockMvc.perform(delete("/books/admin/delete/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Book deleted"));
        verify(service).deleteBook(1L);
    }
}
