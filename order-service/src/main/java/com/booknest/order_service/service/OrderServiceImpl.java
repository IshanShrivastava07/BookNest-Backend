package com.booknest.order_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import com.booknest.order_service.client.CartClient;
import com.booknest.order_service.client.NotificationClient;
import com.booknest.order_service.client.WalletClient;
import com.booknest.order_service.dto.CartItem;
import com.booknest.order_service.dto.NotificationRequest;
import com.booknest.order_service.dto.OrderEvent;
import com.booknest.order_service.dto.OrderRequest;
import com.booknest.order_service.dto.WalletRequest;
import com.booknest.order_service.entity.Address;
import com.booknest.order_service.entity.Order;
import com.booknest.order_service.entity.OrderItem;
import com.booknest.order_service.messaging.OrderEventPublisher;
import com.booknest.order_service.repository.AddressRepository;
import com.booknest.order_service.repository.OrderRepository;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private AddressRepository addressRepo;

    @Autowired
    private CartClient cartClient;

    @Autowired
    private WalletClient walletClient;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private PaymentService paymentService;

    @Override
    @Transactional
    public Order placeOrder(OrderRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("Valid user is required");
        }

        String mode = request.getPaymentMode() == null ? "" : request.getPaymentMode().trim();
        if ("RAZORPAY".equalsIgnoreCase(mode)) {
            if (request.getRazorpayOrderId() == null || request.getRazorpayPaymentId() == null
                    || request.getRazorpaySignature() == null) {
                throw new IllegalArgumentException("Razorpay payment details are required");
            }
            try {
                boolean ok = paymentService.verifySignature(
                        request.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        request.getRazorpaySignature());
                if (!ok) {
                    throw new IllegalArgumentException("Invalid Razorpay signature");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Payment verification failed for userId={}: {}", request.getUserId(), e.getMessage());
                throw new IllegalStateException("Payment verification failed: " + e.getMessage());
            }
        }

        List<CartItem> cartItems = cartClient.getCart();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        double total = 0;
        int totalQuantity = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
            totalQuantity += item.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setBookId(item.getBookId());
            orderItem.setBookTitle(item.getBookTitle());
            orderItem.setAuthor(item.getAuthor());
            orderItem.setCoverImage(item.getCoverImage());
            orderItem.setPrice(item.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);
        }

        if ("WALLET".equalsIgnoreCase(mode)) {
            WalletRequest w = new WalletRequest();
            w.setUserId(request.getUserId());
            w.setAmount(total);
            walletClient.pay(w);
        }

        Address address = new Address();
        address.setUserId(request.getUserId());
        address.setFullName(request.getFullName());
        address.setMobile(request.getMobile());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        addressRepo.save(address);

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmountPaid(total);
        order.setModeOfPayment(normalizePaymentMode(request.getPaymentMode()));
        order.setQuantity(totalQuantity);
        order.setBookLineCount(orderItems.size());
        order.setOrderDate(now);
        order.setEstimatedDeliveryDate(now.toLocalDate().plusDays(7));
        order.setItems(orderItems);

        if ("COD".equalsIgnoreCase(mode)) {
            order.setOrderStatus("PLACED");
        } else if ("WALLET".equalsIgnoreCase(mode) || "ONLINE".equals(order.getModeOfPayment())) {
            order.setOrderStatus("PAID");
        } else {
            order.setOrderStatus("PAID");
        }

        log.info("Saving order for userId={}, amount={}, mode={}", request.getUserId(), total, mode);
        Order savedOrder = orderRepo.save(order);
        log.info("Order placed successfully: orderId={}, userId={}", savedOrder.getOrderId(), request.getUserId());

        cartClient.clearCart();

        publishNotification(request.getUserId(), "ORDER_PLACED", "Your order #" + savedOrder.getOrderId() + " was placed.");
        if ("RAZORPAY".equalsIgnoreCase(mode)) {
            publishNotification(request.getUserId(), "PAYMENT_SUCCESS", "Payment received for order #" + savedOrder.getOrderId() + ".");
        }
        if ("WALLET".equalsIgnoreCase(mode)) {
            publishNotification(request.getUserId(), "PAYMENT_SUCCESS", "Wallet payment applied for order #" + savedOrder.getOrderId() + ".");
        }

        return savedOrder;
    }

    @Override
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepo.findByUserId(userId);
    }

    @Override
    public Order getOrderById(Long orderId, Long userId) {
        return orderRepo.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override
    public boolean userHasPurchasedBook(Long userId, Long bookId) {
        return orderRepo.userHasPurchasedBook(userId, bookId);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        order.setOrderStatus(status.trim().toUpperCase());
        Order saved = orderRepo.save(order);
        log.info("Order status updated: orderId={}, status={}", orderId, status);
        return saved;
    }

    private void publishNotification(Long userId, String type, String message) {
        try {
            orderEventPublisher.publish(new OrderEvent(userId, type, message));
            log.info("Order event published: type={}, userId={}", type, userId);
        } catch (Exception e) {
            log.warn("RabbitMQ publish failed for userId={}, falling back to Feign: {}", userId, e.getMessage());
            try {
                NotificationRequest n = new NotificationRequest();
                n.setUserId(userId);
                n.setMessage(message);
                notificationClient.send(n);
                log.info("Notification sent via Feign fallback for userId={}", userId);
            } catch (Exception e2) {
                log.error("Failed to send notification via Feign fallback for userId={}: {}", userId, e2.getMessage());
            }
        }
    }

    private String normalizePaymentMode(String paymentMode) {
        if (paymentMode == null || paymentMode.isBlank()) {
            return "ONLINE";
        }
        String normalized = paymentMode.trim().toUpperCase();
        if ("RAZORPAY".equals(normalized)) {
            return "ONLINE";
        }
        return normalized;
    }
}
