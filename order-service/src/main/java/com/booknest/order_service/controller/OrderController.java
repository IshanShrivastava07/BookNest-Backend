package com.booknest.order_service.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.booknest.order_service.dto.OrderRequest;
import com.booknest.order_service.dto.PaymentOrderResponse;
import com.booknest.order_service.dto.PaymentVerifyRequest;
import com.booknest.order_service.entity.Order;
import com.booknest.order_service.service.OrderService;
import com.booknest.order_service.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService service;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private HttpServletRequest request;

    private Long getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("SOURCE: ORDER-SERVICE | Missing X-User-Id header");
        }
        return Long.parseLong(userIdStr);
    }

    @PostMapping("/place")
    public Order placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        orderRequest.setUserId(getCurrentUserId());
        return service.placeOrder(orderRequest);
    }

    @PostMapping("/create")
    public Order createOrderLegacy(@Valid @RequestBody OrderRequest orderRequest) {
        orderRequest.setUserId(getCurrentUserId());
        return service.placeOrder(orderRequest);
    }

    @GetMapping
    public List<Order> getOrders() {
        return service.getOrdersByUser(getCurrentUserId());
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return service.getOrderById(id, getCurrentUserId());
    }

    @GetMapping("/verify-purchase/{bookId}")
    public Map<String, Boolean> verifyPurchase(@PathVariable Long bookId) {
        boolean ok = service.userHasPurchasedBook(getCurrentUserId(), bookId);
        return Map.of("purchased", ok);
    }

    @GetMapping("/test-public")
    public String testPublic() {
        return "Order service is reachable! (No Security)";
    }

    @PostMapping("/create-payment")
    public PaymentOrderResponse createPayment(@RequestParam double amount) throws Exception {
        return paymentService.createOrder(amount);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody PaymentVerifyRequest verifyRequest) throws Exception {
        boolean isValid = paymentService.verifySignature(
                verifyRequest.getRazorpayOrderId(),
                verifyRequest.getRazorpayPaymentId(),
                verifyRequest.getRazorpaySignature());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", isValid);
        body.put("message", isValid ? "Payment SUCCESS" : "Payment FAILED");
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateOrderStatus(id, status);
    }
}
