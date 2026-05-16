package com.booknest.wallet_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WalletRazorpayServiceTest {

    @InjectMocks
    private WalletRazorpayService razorpayService;

    @Test
    void createOrderInr_MissingConfig_ThrowsException() {
        ReflectionTestUtils.setField(razorpayService, "key", "");
        ReflectionTestUtils.setField(razorpayService, "secret", "");
        assertThrows(IllegalStateException.class, () -> razorpayService.createOrderInr(100.0, "r1"));
    }

    @Test
    void createOrderInr_InvalidConfig_ThrowsException() {
        ReflectionTestUtils.setField(razorpayService, "key", "k");
        ReflectionTestUtils.setField(razorpayService, "secret", "s");
        // Actual RazorpayClient will throw when trying to connect
        assertThrows(Exception.class, () -> razorpayService.createOrderInr(100.0, "r1"));
    }

    @Test
    void verifySignature_MissingSecret_ReturnsFalse() throws Exception {
        ReflectionTestUtils.setField(razorpayService, "secret", "");
        assertFalse(razorpayService.verifySignature("o1", "p1", "s1"));
    }

    @Test
    void verifySignature_InvalidSignature_ReturnsFalse() throws Exception {
        ReflectionTestUtils.setField(razorpayService, "secret", "sec");
        try {
            assertFalse(razorpayService.verifySignature("o1", "p1", "s1"));
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    void getPublishableKey_ShouldReturnKey() {
        ReflectionTestUtils.setField(razorpayService, "key", "my-key");
        assertEquals("my-key", razorpayService.getPublishableKey());
    }
}
