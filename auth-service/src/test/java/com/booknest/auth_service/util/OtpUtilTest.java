package com.booknest.auth_service.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OtpUtilTest {

    @Test
    void generateOtp_ShouldReturnSixDigitString() {
        String otp = OtpUtil.generateOtp();
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void generateOtp_ShouldBeRandom() {
        String otp1 = OtpUtil.generateOtp();
        String otp2 = OtpUtil.generateOtp();
        // Extremely low probability of being equal, but technically possible.
        // For a simple unit test, this is usually acceptable.
        assertNotEquals(otp1, otp2);
    }
}
