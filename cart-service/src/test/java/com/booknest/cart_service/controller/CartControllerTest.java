package com.booknest.cart_service.controller;

import com.booknest.cart_service.dto.CartRequest;
import com.booknest.cart_service.entity.Cart;
import com.booknest.cart_service.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CartController cartController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(cartController, "request", request);
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
    }

    @Test
    void addItem() throws Exception {
        Cart cart = new Cart();
        cart.setCartId(1L);
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.addItem(any(CartRequest.class))).thenReturn(cart);

        mockMvc.perform(post("/cart")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":1, \"quantity\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_IllegalArgument() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.addItem(any(CartRequest.class))).thenThrow(new IllegalArgumentException("Error"));

        mockMvc.perform(post("/cart")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":1, \"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void addItem_Exception() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.addItem(any(CartRequest.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/cart")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":1, \"quantity\":1}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void addByBookId() throws Exception {
        Cart cart = new Cart();
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.addItem(any(CartRequest.class))).thenReturn(cart);

        mockMvc.perform(post("/cart/add/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isOk());
    }
    
    @Test
    void addByBookId_IllegalArgument() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.addItem(any(CartRequest.class))).thenThrow(new IllegalArgumentException("Error"));

        mockMvc.perform(post("/cart/add/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void addByBookId_Exception() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.addItem(any(CartRequest.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/cart/add/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getItems() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.getCartItems(1L)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/cart")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateQuantity() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        mockMvc.perform(put("/cart/item/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isOk());
        verify(cartService).updateQuantity(10L, 1L, 2);
    }
    
    @Test
    void updateQuantity_IllegalArgument() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.updateQuantity(10L, 1L, 2)).thenThrow(new IllegalArgumentException("Error"));
        mockMvc.perform(put("/cart/item/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateByBookId() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        mockMvc.perform(put("/cart/update/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isOk());
    }
    
    @Test
    void updateByBookId_IllegalArgument() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(cartService.updateQuantityByBookId(1L, 10L, 2)).thenThrow(new IllegalArgumentException("Error"));
        mockMvc.perform(put("/cart/update/10")
                .header("X-User-Id", "1")
                .param("quantity", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeItem() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        mockMvc.perform(delete("/cart/item/10")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
        verify(cartService).removeItem(10L, 1L);
    }
    
    @Test
    void removeItem_IllegalArgument() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Error")).when(cartService).removeItem(10L, 1L);
        mockMvc.perform(delete("/cart/item/10")
                .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void removeItem_Exception() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        org.mockito.Mockito.doThrow(new RuntimeException("Error")).when(cartService).removeItem(10L, 1L);
        mockMvc.perform(delete("/cart/item/10")
                .header("X-User-Id", "1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void removeByBookId() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        mockMvc.perform(delete("/cart/remove/10")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }
    
    @Test
    void removeByBookId_IllegalArgument() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Error")).when(cartService).removeByBookId(1L, 10L);
        mockMvc.perform(delete("/cart/remove/10")
                .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearCart() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        mockMvc.perform(delete("/cart/clear")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
        verify(cartService).clearCart(10L);
    }
    
    @Test
    void clearCart_Exception() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        org.mockito.Mockito.doThrow(new RuntimeException("Error")).when(cartService).clearCart(10L);
        mockMvc.perform(delete("/cart/clear")
                .header("X-User-Id", "10"))
                .andExpect(status().isInternalServerError());
    }
    

}
