package com.booknest.wishlist_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booknest.wishlist_service.client.BookClient;
import com.booknest.wishlist_service.client.CartClient;
import com.booknest.wishlist_service.dto.BookDto;
import com.booknest.wishlist_service.entity.Wishlist;
import com.booknest.wishlist_service.entity.WishlistItem;
import com.booknest.wishlist_service.repository.WishlistItemRepository;
import com.booknest.wishlist_service.repository.WishlistRepository;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepo;

    @Autowired
    private WishlistItemRepository itemRepo;

    @Autowired
    private BookClient bookClient;

    @Autowired
    private CartClient cartClient;

    private Wishlist getOrCreateWishlist(Long userId) {
        Wishlist wishlist = wishlistRepo.findByUserId(userId);
        if (wishlist == null) {
            wishlist = new Wishlist();
            wishlist.setUserId(userId);
            wishlist = wishlistRepo.save(wishlist);
        }
        return wishlist;
    }

    @Override
    @Transactional
    public WishlistItem addBook(Long userId, Long bookId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        if (itemRepo.findByWishlistIdAndBookId(wishlist.getWishlistId(), bookId).isPresent()) {
            throw new IllegalStateException("Book already in wishlist");
        }
        BookDto book = bookClient.getBookById(bookId);
        WishlistItem item = new WishlistItem();
        item.setWishlistId(wishlist.getWishlistId());
        item.setBookId(book.getBookId());
        item.setBookTitle(book.getTitle());
        item.setPrice(book.getPrice());
        return itemRepo.save(item);
    }

    @Override
    public List<WishlistItem> getWishlist(Long userId) {
        Wishlist wishlist = wishlistRepo.findByUserId(userId);
        if (wishlist == null) {
            return List.of();
        }
        return itemRepo.findByWishlistId(wishlist.getWishlistId());
    }

    @Override
    @Transactional
    public void removeByBookId(Long userId, Long bookId) {
        Wishlist wishlist = wishlistRepo.findByUserId(userId);
        if (wishlist == null) {
            return;
        }
        itemRepo.deleteByWishlistIdAndBookId(wishlist.getWishlistId(), bookId);
    }

    @Override
    @Transactional
    public void moveToCart(Long userId, Long bookId) {
        Wishlist wishlist = wishlistRepo.findByUserId(userId);
        if (wishlist == null) {
            throw new IllegalArgumentException("Wishlist is empty");
        }
        WishlistItem item = itemRepo.findByWishlistIdAndBookId(wishlist.getWishlistId(), bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not in wishlist"));
        cartClient.addToCart(bookId, 1);
        itemRepo.delete(item);
    }
}
