package com.booknest.wishlist_service.service;

import com.booknest.wishlist_service.client.BookClient;
import com.booknest.wishlist_service.client.CartClient;
import com.booknest.wishlist_service.dto.BookDto;
import com.booknest.wishlist_service.entity.Wishlist;
import com.booknest.wishlist_service.entity.WishlistItem;
import com.booknest.wishlist_service.repository.WishlistItemRepository;
import com.booknest.wishlist_service.repository.WishlistRepository;
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
class WishlistServiceImplTest {

    @Mock private WishlistRepository wishlistRepo;
    @Mock private WishlistItemRepository itemRepo;
    @Mock private BookClient bookClient;
    @Mock private CartClient cartClient;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private Wishlist wishlist;
    private WishlistItem wishlistItem;
    private BookDto book;

    @BeforeEach
    void setUp() {
        wishlist = new Wishlist();
        wishlist.setWishlistId(1L);
        wishlist.setUserId(10L);

        wishlistItem = new WishlistItem();
        wishlistItem.setWishlistId(1L);
        wishlistItem.setBookId(100L);
        wishlistItem.setBookTitle("Clean Code");
        wishlistItem.setPrice(350.0);

        book = new BookDto();
        book.setBookId(100L);
        book.setTitle("Clean Code");
        book.setPrice(350.0);
    }

    // ─── addBook ─────────────────────────────────────────────────────────────

    @Test
    void addBook_NewItem_ShouldSave() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(wishlist);
        when(itemRepo.findByWishlistIdAndBookId(1L, 100L)).thenReturn(Optional.empty());
        when(bookClient.getBookById(100L)).thenReturn(book);
        when(itemRepo.save(any(WishlistItem.class))).thenReturn(wishlistItem);

        WishlistItem result = wishlistService.addBook(10L, 100L);

        assertNotNull(result);
        verify(itemRepo).save(any(WishlistItem.class));
    }

    @Test
    void addBook_DuplicateBook_ShouldThrow() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(wishlist);
        when(itemRepo.findByWishlistIdAndBookId(1L, 100L)).thenReturn(Optional.of(wishlistItem));

        assertThrows(IllegalStateException.class, () -> wishlistService.addBook(10L, 100L));
    }

    @Test
    void addBook_CreatesWishlistIfAbsent() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(null);
        when(wishlistRepo.save(any(Wishlist.class))).thenReturn(wishlist);
        when(itemRepo.findByWishlistIdAndBookId(1L, 100L)).thenReturn(Optional.empty());
        when(bookClient.getBookById(100L)).thenReturn(book);
        when(itemRepo.save(any(WishlistItem.class))).thenReturn(wishlistItem);

        WishlistItem result = wishlistService.addBook(10L, 100L);

        assertNotNull(result);
        verify(wishlistRepo).save(any(Wishlist.class));
    }

    // ─── getWishlist ─────────────────────────────────────────────────────────

    @Test
    void getWishlist_NotFound_ShouldReturnEmpty() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(null);

        List<WishlistItem> result = wishlistService.getWishlist(10L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getWishlist_WithItems_ShouldReturnItems() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(wishlist);
        when(itemRepo.findByWishlistId(1L)).thenReturn(List.of(wishlistItem));

        List<WishlistItem> result = wishlistService.getWishlist(10L);

        assertEquals(1, result.size());
    }

    // ─── removeByBookId ──────────────────────────────────────────────────────

    @Test
    void removeByBookId_WishlistNotFound_ShouldDoNothing() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(null);

        wishlistService.removeByBookId(10L, 100L);

        verifyNoInteractions(itemRepo);
    }

    @Test
    void removeByBookId_Exists_ShouldDelete() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(wishlist);

        wishlistService.removeByBookId(10L, 100L);

        verify(itemRepo).deleteByWishlistIdAndBookId(1L, 100L);
    }

    // ─── moveToCart ──────────────────────────────────────────────────────────

    @Test
    void moveToCart_WishlistNotFound_ShouldThrow() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> wishlistService.moveToCart(10L, 100L));
    }

    @Test
    void moveToCart_BookNotInWishlist_ShouldThrow() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(wishlist);
        when(itemRepo.findByWishlistIdAndBookId(1L, 100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> wishlistService.moveToCart(10L, 100L));
    }

    @Test
    void moveToCart_Valid_ShouldAddToCartAndRemoveFromWishlist() {
        when(wishlistRepo.findByUserId(10L)).thenReturn(wishlist);
        when(itemRepo.findByWishlistIdAndBookId(1L, 100L)).thenReturn(Optional.of(wishlistItem));

        wishlistService.moveToCart(10L, 100L);

        verify(cartClient).addToCart(100L, 1);
        verify(itemRepo).delete(wishlistItem);
    }
}
