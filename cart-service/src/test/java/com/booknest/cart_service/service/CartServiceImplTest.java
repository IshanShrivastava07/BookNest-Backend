package com.booknest.cart_service.service;

import com.booknest.cart_service.client.BookClient;
import com.booknest.cart_service.dto.Book;
import com.booknest.cart_service.dto.CartRequest;
import com.booknest.cart_service.entity.Cart;
import com.booknest.cart_service.entity.CartItem;
import com.booknest.cart_service.repository.CartItemRepository;
import com.booknest.cart_service.repository.CartRepository;
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
class CartServiceImplTest {

    @Mock private CartRepository cartRepo;
    @Mock private CartItemRepository itemRepo;
    @Mock private BookClient bookClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private CartItem cartItem;
    private Book book;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId(1L);
        cart.setUserId(10L);
        cart.setTotalPrice(0.0);

        cartItem = new CartItem();
        cartItem.setItemId(1L);
        cartItem.setCartId(1L);
        cartItem.setBookId(100L);
        cartItem.setBookTitle("Clean Code");
        cartItem.setPrice(350.0);
        cartItem.setQuantity(1);

        book = new Book();
        book.setBookId(100L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setPrice(350.0);
        book.setCoverImage("cover.jpg");
    }

    // ─── addItem ─────────────────────────────────────────────────────────────

    @Test
    void addItem_NullRequest_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(null));
    }

    @Test
    void addItem_NullUserId_ShouldThrow() {
        CartRequest req = new CartRequest();
        req.setBookId(100L);
        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(req));
    }

    @Test
    void addItem_NewItem_ShouldCreateAndSave() {
        CartRequest req = new CartRequest();
        req.setUserId(10L);
        req.setBookId(100L);
        req.setQuantity(2);

        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(bookClient.getBookById(100L)).thenReturn(book);
        when(itemRepo.findByCartIdAndBookId(1L, 100L)).thenReturn(Optional.empty());
        when(itemRepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.addItem(req);

        assertNotNull(result);
        verify(itemRepo).save(any(CartItem.class));
    }

    @Test
    void addItem_ExistingItem_ShouldIncrementQuantity() {
        CartRequest req = new CartRequest();
        req.setUserId(10L);
        req.setBookId(100L);
        req.setQuantity(1);

        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(bookClient.getBookById(100L)).thenReturn(book);
        when(itemRepo.findByCartIdAndBookId(1L, 100L)).thenReturn(Optional.of(cartItem));
        when(itemRepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);

        cartService.addItem(req);

        assertEquals(2, cartItem.getQuantity()); // was 1, +1 = 2
    }

    // ─── getCartItems ─────────────────────────────────────────────────────────

    @Test
    void getCartItems_CartNotFound_ShouldReturnEmpty() {
        when(cartRepo.findByUserId(10L)).thenReturn(null);

        List<CartItem> result = cartService.getCartItems(10L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCartItems_WithItems_ShouldReturnItems() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(bookClient.getBookById(100L)).thenReturn(book); // same price, no update needed

        List<CartItem> result = cartService.getCartItems(10L);

        assertEquals(1, result.size());
    }

    // ─── updateQuantity ──────────────────────────────────────────────────────

    @Test
    void updateQuantity_InvalidQty_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantity(1L, 10L, 0));
    }

    @Test
    void updateQuantity_ItemNotOwned_ShouldThrow() {
        Cart anotherCart = new Cart();
        anotherCart.setCartId(1L);
        anotherCart.setUserId(99L); // different user

        when(itemRepo.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepo.findById(1L)).thenReturn(Optional.of(anotherCart));

        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantity(1L, 10L, 3));
    }

    @Test
    void updateQuantity_Valid_ShouldUpdateAndReturn() {
        when(itemRepo.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepo.findById(1L)).thenReturn(Optional.of(cart));
        when(itemRepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);

        CartItem result = cartService.updateQuantity(1L, 10L, 5);

        assertEquals(5, result.getQuantity());
    }

    // ─── removeItem ──────────────────────────────────────────────────────────

    @Test
    void removeItem_Valid_ShouldDelete() {
        when(itemRepo.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepo.findById(1L)).thenReturn(Optional.of(cart));
        when(itemRepo.findByCartId(1L)).thenReturn(List.of());
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);

        cartService.removeItem(1L, 10L);

        verify(itemRepo).deleteById(1L);
    }

    // ─── clearCart ───────────────────────────────────────────────────────────

    @Test
    void clearCart_CartExists_ShouldClearAllItems() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(10L);

        verify(itemRepo).delete(cartItem);
        assertEquals(0.0, cart.getTotalPrice());
    }

    @Test
    void clearCart_CartNotFound_ShouldDoNothing() {
        when(cartRepo.findByUserId(10L)).thenReturn(null);

        cartService.clearCart(10L);

        verifyNoInteractions(itemRepo);
    }
    
    // ─── updateQuantityByBookId ────────────────────────────────────────────────
    
    @Test
    void updateQuantityByBookId_InvalidQty_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantityByBookId(10L, 100L, 0));
    }
    
    @Test
    void updateQuantityByBookId_CartNotFound_ShouldThrow() {
        when(cartRepo.findByUserId(10L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantityByBookId(10L, 100L, 2));
    }
    
    @Test
    void updateQuantityByBookId_ItemNotFound_ShouldThrow() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartIdAndBookId(1L, 100L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantityByBookId(10L, 100L, 2));
    }
    
    @Test
    void updateQuantityByBookId_Valid_ShouldUpdate() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartIdAndBookId(1L, 100L)).thenReturn(Optional.of(cartItem));
        when(itemRepo.save(any(CartItem.class))).thenReturn(cartItem);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);
        
        CartItem result = cartService.updateQuantityByBookId(10L, 100L, 5);
        assertEquals(5, result.getQuantity());
    }
    
    // ─── removeByBookId ────────────────────────────────────────────────────────
    
    @Test
    void removeByBookId_CartNotFound_ShouldReturnSilently() {
        when(cartRepo.findByUserId(10L)).thenReturn(null);
        assertDoesNotThrow(() -> cartService.removeByBookId(10L, 100L));
    }
    
    @Test
    void removeByBookId_ItemNotFound_ShouldReturnSilently() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartIdAndBookId(1L, 100L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> cartService.removeByBookId(10L, 100L));
    }
    
    @Test
    void removeByBookId_Valid_ShouldDelete() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartIdAndBookId(1L, 100L)).thenReturn(Optional.of(cartItem));
        when(itemRepo.findByCartId(1L)).thenReturn(List.of());
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);
        
        cartService.removeByBookId(10L, 100L);
        verify(itemRepo).delete(cartItem);
    }
    
    // ─── Price Sync ─────────────────────────────────────────────────────────────
    
    @Test
    void getCartItems_PriceChanged_ShouldUpdatePrice() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        book.setPrice(400.0); // Book price increased
        when(bookClient.getBookById(100L)).thenReturn(book);
        
        List<CartItem> result = cartService.getCartItems(10L);
        assertEquals(400.0, result.get(0).getPrice());
        verify(itemRepo).save(cartItem);
    }
    
    @Test
    void getCartItems_BookClientFails_ShouldKeepOldPrice() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(bookClient.getBookById(100L)).thenThrow(new RuntimeException("Service down"));
        
        List<CartItem> result = cartService.getCartItems(10L);
        assertEquals(350.0, result.get(0).getPrice());
    }
    
    @Test
    void getCartItems_BookNotFound_ShouldKeepOldPrice() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(bookClient.getBookById(100L)).thenReturn(null);
        
        List<CartItem> result = cartService.getCartItems(10L);
        assertEquals(350.0, result.get(0).getPrice());
    }
    
    @Test
    void getCartItems_EmptyItems_ShouldReturnEmpty() {
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(itemRepo.findByCartId(1L)).thenReturn(List.of());
        List<CartItem> result = cartService.getCartItems(10L);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void updateQuantity_ItemNotFound() {
        when(itemRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> cartService.updateQuantity(1L, 10L, 5));
    }
    
    @Test
    void removeItem_ItemNotFound() {
        when(itemRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> cartService.removeItem(1L, 10L));
    }
    
    @Test
    void removeItem_ItemNotOwned() {
        Cart anotherCart = new Cart();
        anotherCart.setCartId(2L);
        anotherCart.setUserId(99L);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepo.findById(1L)).thenReturn(Optional.of(anotherCart));
        assertThrows(IllegalArgumentException.class, () -> cartService.removeItem(1L, 10L));
    }
    
    @Test
    void addItem_BookClientFails() {
        CartRequest req = new CartRequest();
        req.setUserId(10L);
        req.setBookId(100L);
        req.setQuantity(2);
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(bookClient.getBookById(100L)).thenThrow(new RuntimeException("Service down"));
        assertThrows(RuntimeException.class, () -> cartService.addItem(req));
    }
    
    @Test
    void addItem_BookNotFound() {
        CartRequest req = new CartRequest();
        req.setUserId(10L);
        req.setBookId(100L);
        req.setQuantity(2);
        when(cartRepo.findByUserId(10L)).thenReturn(cart);
        when(bookClient.getBookById(100L)).thenReturn(null);
        assertThrows(NullPointerException.class, () -> cartService.addItem(req));
    }
}
