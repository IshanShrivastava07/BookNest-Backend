package com.booknest.cart_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.booknest.cart_service.dto.Book;
import com.booknest.cart_service.dto.CartRequest;
import com.booknest.cart_service.entity.Cart;
import com.booknest.cart_service.entity.CartItem;
import com.booknest.cart_service.repository.CartItemRepository;
import com.booknest.cart_service.repository.CartRepository;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepo;
    @Autowired
    private CartItemRepository itemRepo;

    @Autowired
    private com.booknest.cart_service.client.BookClient bookClient;

    private Cart getOrCreateCart(Long userId) {
        Cart cart = cartRepo.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setTotalPrice(0.0);
            cart = cartRepo.save(cart);
        }
        return cart;
    }

    private void assertCartOwner(CartItem item, Long userId) {
        Cart cart = cartRepo.findById(item.getCartId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        if (!cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not allowed to modify this cart item");
        }
    }

    @Override
    public Cart addItem(CartRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cart request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request.getBookId() == null) {
            log.warn("Cart operation rejected: Missing bookId for userId={}", request.getUserId());
            throw new IllegalArgumentException("Book ID cannot be null");
        }

        Cart cart = getOrCreateCart(request.getUserId());

        Book book = bookClient.getBookById(request.getBookId());

        CartItem item = itemRepo.findByCartIdAndBookId(cart.getCartId(), request.getBookId()).orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setCartId(cart.getCartId());
            item.setBookId(book.getBookId());
            item.setBookTitle(book.getTitle());
            item.setAuthor(book.getAuthor());
            item.setCoverImage(book.getCoverImage());
            item.setPrice(book.getPrice());
            item.setQuantity(Math.max(request.getQuantity(), 1));
        } else {
            item.setQuantity(item.getQuantity() + Math.max(request.getQuantity(), 1));
            item.setPrice(book.getPrice());
        }
        itemRepo.save(item);

        recalculateAndSave(cart);
        log.info("Item added to cart for userId={}, bookId={}, quantity={}", request.getUserId(), request.getBookId(), request.getQuantity());
        return cart;
    }

    @Override
    public List<CartItem> getCartItems(Long userId) {
        Cart cart = cartRepo.findByUserId(userId);
        if (cart == null) {
            return List.of();
        }
        List<CartItem> items = itemRepo.findByCartId(cart.getCartId());
        boolean updated = false;
        
        for (CartItem item : items) {
            try {
                Book book = bookClient.getBookById(item.getBookId());
                if (book != null && (book.getPrice() != item.getPrice() || !book.getTitle().equals(item.getBookTitle()) || !book.getCoverImage().equals(item.getCoverImage()))) {
                    item.setPrice(book.getPrice());
                    item.setBookTitle(book.getTitle());
                    item.setCoverImage(book.getCoverImage());
                    itemRepo.save(item);
                    updated = true;
                }
            } catch (Exception e) {
                // If book fetch fails, just keep the existing data
            }
        }
        
        if (updated) {
            recalculateAndSave(cart);
        }
        
        return items;
    }

    @Override
    public CartItem updateQuantity(Long itemId, Long userId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        CartItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        assertCartOwner(item, userId);
        item.setQuantity(quantity);
        CartItem saved = itemRepo.save(item);
        Cart cart = cartRepo.findById(item.getCartId()).orElse(null);
        if (cart != null) {
            recalculateAndSave(cart);
        }
        return saved;
    }

    @Override
    public CartItem updateQuantityByBookId(Long userId, Long bookId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        Cart cart = cartRepo.findByUserId(userId);
        if (cart == null) {
            throw new IllegalArgumentException("Cart is empty");
        }
        CartItem item = itemRepo.findByCartIdAndBookId(cart.getCartId(), bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not in cart"));
        item.setQuantity(quantity);
        CartItem saved = itemRepo.save(item);
        recalculateAndSave(cart);
        return saved;
    }

    @Override
    public void removeItem(Long itemId, Long userId) {
        CartItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        assertCartOwner(item, userId);
        Long cartId = item.getCartId();
        itemRepo.deleteById(itemId);
        Cart cart = cartRepo.findById(cartId).orElse(null);
        if (cart != null) {
            recalculateAndSave(cart);
        }
        log.info("Item removed from cart: itemId={}, userId={}", itemId, userId);
    }

    @Override
    public void removeByBookId(Long userId, Long bookId) {
        Cart cart = cartRepo.findByUserId(userId);
        if (cart == null) {
            return;
        }
        CartItem item = itemRepo.findByCartIdAndBookId(cart.getCartId(), bookId).orElse(null);
        if (item == null) {
            return;
        }
        itemRepo.delete(item);
        recalculateAndSave(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepo.findByUserId(userId);

        if (cart == null) {
            return;
        }

        List<CartItem> items = itemRepo.findByCartId(cart.getCartId());
        for (CartItem item : items) {
            itemRepo.delete(item);
        }

        cart.setTotalPrice(0);
        cartRepo.save(cart);
        log.info("Cart cleared for userId={}", userId);
    }

    private void recalculateAndSave(Cart cart) {
        double total = itemRepo.findByCartId(cart.getCartId())
                .stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(total);
        cartRepo.save(cart);
    }
}
