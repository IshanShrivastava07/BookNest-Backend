package com.booknest.order_service.controller;

import com.booknest.order_service.dto.OrderRequest;
import com.booknest.order_service.entity.Order;
import com.booknest.order_service.service.OrderService;
import com.booknest.order_service.service.PaymentService;
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
class OrderControllerTest {

    @Mock
    private OrderService orderService;
    
    @Mock
    private PaymentService paymentService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(orderController, "request", request);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    void placeOrder() throws Exception {
        Order order = new Order();
        order.setOrderId(1L);
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/orders/place")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"John Doe\", \"mobile\":\"9876543210\", \"city\":\"Test City\", \"state\":\"Test State\", \"pincode\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrders() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(orderService.getOrdersByUser(1L)).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/orders")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderById() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(orderService.getOrderById(1L, 1L)).thenReturn(new Order());
        mockMvc.perform(get("/orders/1")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateOrderStatus() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), eq("SHIPPED"))).thenReturn(new Order());
        mockMvc.perform(put("/orders/1/status")
                .param("status", "SHIPPED"))
                .andExpect(status().isOk());
    }

    @Test
    void createOrderLegacy() throws Exception {
        Order order = new Order();
        order.setOrderId(2L);
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/orders/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"John Doe\", \"mobile\":\"9876543210\", \"city\":\"Test City\", \"state\":\"Test State\", \"pincode\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void verifyPurchase() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(orderService.userHasPurchasedBook(1L, 100L)).thenReturn(true);
        mockMvc.perform(get("/orders/verify-purchase/100")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(true));
    }

    @Test
    void testPublic() throws Exception {
        mockMvc.perform(get("/orders/test-public"))
                .andExpect(status().isOk())
                .andExpect(content().string("Order service is reachable! (No Security)"));
    }

    @Test
    void createPayment() throws Exception {
        com.booknest.order_service.dto.PaymentOrderResponse resp = 
            new com.booknest.order_service.dto.PaymentOrderResponse("order_123", 500, "INR", "receipt_1");
        when(paymentService.createOrder(500.0)).thenReturn(resp);

        mockMvc.perform(post("/orders/create-payment")
                .param("amount", "500.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_123"));
    }

    @Test
    void verifyPayment_Success() throws Exception {
        when(paymentService.verifySignature("order_123", "pay_123", "sig_123")).thenReturn(true);

        mockMvc.perform(post("/orders/verify-payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"razorpayOrderId\":\"order_123\", \"razorpayPaymentId\":\"pay_123\", \"razorpaySignature\":\"sig_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void verifyPayment_Failure() throws Exception {
        when(paymentService.verifySignature("order_123", "pay_123", "sig_123")).thenReturn(false);

        mockMvc.perform(post("/orders/verify-payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"razorpayOrderId\":\"order_123\", \"razorpayPaymentId\":\"pay_123\", \"razorpaySignature\":\"sig_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
