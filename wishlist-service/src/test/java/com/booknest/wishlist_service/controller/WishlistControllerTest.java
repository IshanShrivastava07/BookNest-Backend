package com.booknest.wishlist_service.controller;

import com.booknest.wishlist_service.entity.WishlistItem;
import com.booknest.wishlist_service.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WishlistControllerTest {

    @Mock private WishlistService service;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private WishlistController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "request", request);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getWishlist_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(service.getWishlist(10L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/wishlist")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void add_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(service.addBook(anyLong(), anyLong())).thenReturn(new WishlistItem());

        mockMvc.perform(post("/wishlist/add/1")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void add_MissingHeader_ReturnsBadRequest() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        mockMvc.perform(post("/wishlist/add/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void remove_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");

        mockMvc.perform(delete("/wishlist/remove/1")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void moveToCart_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");

        mockMvc.perform(post("/wishlist/move-to-cart/1")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
    }
}
