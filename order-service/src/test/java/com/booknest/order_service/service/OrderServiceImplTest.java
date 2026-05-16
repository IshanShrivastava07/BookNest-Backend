package com.booknest.order_service.service;

import com.booknest.order_service.client.CartClient;
import com.booknest.order_service.client.NotificationClient;
import com.booknest.order_service.client.WalletClient;
import com.booknest.order_service.dto.CartItem;
import com.booknest.order_service.dto.OrderRequest;
import com.booknest.order_service.entity.Order;
import com.booknest.order_service.messaging.OrderEventPublisher;
import com.booknest.order_service.repository.AddressRepository;
import com.booknest.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepo;
    @Mock private AddressRepository addressRepo;
    @Mock private CartClient cartClient;
    @Mock private WalletClient walletClient;
    @Mock private NotificationClient notificationClient;
    @Mock private OrderEventPublisher orderEventPublisher;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequest walletOrderRequest;
    private CartItem cartItem;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        cartItem = new CartItem();
        cartItem.setBookId(1L);
        cartItem.setBookTitle("Clean Code");
        cartItem.setAuthor("Robert Martin");
        cartItem.setPrice(350.0);
        cartItem.setQuantity(2);

        walletOrderRequest = new OrderRequest();
        walletOrderRequest.setUserId(1L);
        walletOrderRequest.setPaymentMode("WALLET");
        walletOrderRequest.setFullName("Test User");
        walletOrderRequest.setMobile("9999999999");
        walletOrderRequest.setCity("Mumbai");
        walletOrderRequest.setState("Maharashtra");
        walletOrderRequest.setPincode("400001");

        savedOrder = new Order();
        savedOrder.setOrderId(100L);
        savedOrder.setUserId(1L);
        savedOrder.setOrderStatus("PAID");
        savedOrder.setAmountPaid(700.0);
    }

    // ─── placeOrder ──────────────────────────────────────────────────────────

    @Test
    void placeOrder_NullRequest_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(null));
    }

    @Test
    void placeOrder_NullUserId_ShouldThrow() {
        walletOrderRequest.setUserId(null);
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void placeOrder_EmptyCart_ShouldThrow() {
        when(cartClient.getCart()).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void placeOrder_WalletMode_ShouldDebitAndSaveOrder() {
        when(cartClient.getCart()).thenReturn(List.of(cartItem));
        when(addressRepo.save(any())).thenReturn(null);
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartClient).clearCart();
        doNothing().when(orderEventPublisher).publish(any());

        Order result = orderService.placeOrder(walletOrderRequest);

        assertEquals(100L, result.getOrderId());
        assertEquals("PAID", result.getOrderStatus());
        verify(walletClient).pay(any());
        verify(cartClient).clearCart();
    }

    @Test
    void placeOrder_WalletFails_ShouldPropagateException() {
        when(cartClient.getCart()).thenReturn(List.of(cartItem));
        doThrow(new RuntimeException("Insufficient balance")).when(walletClient).pay(any());

        assertThrows(RuntimeException.class, () -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void placeOrder_RazorpayMode_InvalidSignature_ShouldThrow() {
        walletOrderRequest.setPaymentMode("RAZORPAY");
        walletOrderRequest.setRazorpayOrderId("order_abc");
        walletOrderRequest.setRazorpayPaymentId("pay_xyz");
        walletOrderRequest.setRazorpaySignature("bad_sig");

        try {
            when(paymentService.verifySignature("order_abc", "pay_xyz", "bad_sig")).thenReturn(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void placeOrder_RazorpayMode_MissingDetails_ShouldThrow() {
        walletOrderRequest.setPaymentMode("RAZORPAY");
        // Missing razorpayOrderId, paymentId, signature

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void placeOrder_CodMode_ShouldSetPlacedStatus() {
        walletOrderRequest.setPaymentMode("COD");
        Order codOrder = new Order();
        codOrder.setOrderId(101L);
        codOrder.setOrderStatus("PLACED");

        when(cartClient.getCart()).thenReturn(List.of(cartItem));
        when(addressRepo.save(any())).thenReturn(null);
        when(orderRepo.save(any(Order.class))).thenReturn(codOrder);
        doNothing().when(cartClient).clearCart();
        doNothing().when(orderEventPublisher).publish(any());

        Order result = orderService.placeOrder(walletOrderRequest);

        assertEquals("PLACED", result.getOrderStatus());
        verify(walletClient, never()).pay(any());
    }

    @Test
    void placeOrder_RabbitMqFails_ShouldFallbackToFeignNotification() {
        when(cartClient.getCart()).thenReturn(List.of(cartItem));
        when(addressRepo.save(any())).thenReturn(null);
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartClient).clearCart();
        doThrow(new RuntimeException("RabbitMQ down")).when(orderEventPublisher).publish(any());

        // Should not throw — fallback is silent
        assertDoesNotThrow(() -> orderService.placeOrder(walletOrderRequest));
    }

    // ─── getOrdersByUser / getOrderById ──────────────────────────────────────

    @Test
    void getOrdersByUser_ShouldReturnList() {
        when(orderRepo.findByUserId(1L)).thenReturn(List.of(savedOrder));

        List<Order> orders = orderService.getOrdersByUser(1L);

        assertEquals(1, orders.size());
    }

    @Test
    void getOrderById_NotFound_ShouldThrow() {
        when(orderRepo.findByOrderIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderById(999L, 1L));
    }

    // ─── updateOrderStatus ───────────────────────────────────────────────────

    @Test
    void updateOrderStatus_Valid_ShouldUpdateStatus() {
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.updateOrderStatus(100L, "shipped");

        assertEquals("SHIPPED", result.getOrderStatus());
    }

    @Test
    void updateOrderStatus_OrderNotFound_ShouldThrow() {
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatus(999L, "SHIPPED"));
    }

    @Test
    void updateOrderStatus_BlankStatus_ShouldThrow() {
        when(orderRepo.findById(100L)).thenReturn(Optional.of(savedOrder));

        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatus(100L, "  "));
    }

    @Test
    void placeOrder_RazorpayMode_VerificationThrowsException_ShouldThrow() throws Exception {
        walletOrderRequest.setPaymentMode("RAZORPAY");
        walletOrderRequest.setRazorpayOrderId("order_abc");
        walletOrderRequest.setRazorpayPaymentId("pay_xyz");
        walletOrderRequest.setRazorpaySignature("sig");

        when(paymentService.verifySignature(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Razorpay internal error"));

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void placeOrder_CodMode_BlankPaymentMode_ShouldUseDefault() {
        walletOrderRequest.setPaymentMode("");
        when(cartClient.getCart()).thenReturn(List.of(cartItem));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.placeOrder(walletOrderRequest);
        assertNotNull(result);
        assertEquals("PAID", result.getOrderStatus());
    }

    @Test
    void placeOrder_RabbitMqAndFeignFails_ShouldLogAndNotThrow() {
        when(cartClient.getCart()).thenReturn(List.of(cartItem));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);
        
        doThrow(new RuntimeException("RabbitMQ down")).when(orderEventPublisher).publish(any());
        doThrow(new RuntimeException("Feign down")).when(notificationClient).send(any());

        assertDoesNotThrow(() -> orderService.placeOrder(walletOrderRequest));
    }

    @Test
    void userHasPurchasedBook_ShouldCallRepo() {
        when(orderRepo.userHasPurchasedBook(1L, 1L)).thenReturn(true);
        assertTrue(orderService.userHasPurchasedBook(1L, 1L));
        verify(orderRepo).userHasPurchasedBook(1L, 1L);
    }
}
