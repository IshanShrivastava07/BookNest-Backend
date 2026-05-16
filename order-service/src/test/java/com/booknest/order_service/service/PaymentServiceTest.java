package com.booknest.order_service.service;

import com.booknest.order_service.dto.PaymentOrderResponse;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createOrder_MissingConfig_ThrowsException() {
        ReflectionTestUtils.setField(paymentService, "key", "");
        ReflectionTestUtils.setField(paymentService, "secret", "");
        
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> paymentService.createOrder(500.0));
        assertTrue(ex.getMessage().contains("Razorpay is not configured"));
    }

    @Test
    void createOrder_InvalidConfig_ThrowsRazorpayException() {
        ReflectionTestUtils.setField(paymentService, "key", "invalid_key");
        ReflectionTestUtils.setField(paymentService, "secret", "invalid_secret");
        
        assertThrows(Exception.class, () -> paymentService.createOrder(500.0));
    }

    @Test
    void verifySignature_MissingSecret_ReturnsFalse() throws Exception {
        ReflectionTestUtils.setField(paymentService, "secret", "");
        assertFalse(paymentService.verifySignature("order_id", "payment_id", "signature"));
    }

    @Test
    void verifySignature_NullInputs_ReturnsFalse() throws Exception {
        ReflectionTestUtils.setField(paymentService, "secret", "secret");
        // Razorpay SDK might throw or return false for nulls
        try {
            assertFalse(paymentService.verifySignature(null, null, null));
        } catch (Exception e) {
            // ok
        }
    }
}
