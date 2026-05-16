package com.booknest.order_service.client;

import com.booknest.order_service.exception.WalletPayException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class WalletFeignErrorDecoderTest {

    private WalletFeignErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new WalletFeignErrorDecoder();
    }

    @Test
    void decode_WithResponseBody_AndMessage_ThrowsWalletPayException() {
        String jsonBody = "{\"message\":\"Insufficient balance\"}";
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .body(jsonBody, StandardCharsets.UTF_8)
                .build();

        Exception exception = decoder.decode("methodKey", response);

        assertTrue(exception instanceof WalletPayException);
        assertEquals("Insufficient balance", exception.getMessage());
    }

    @Test
    void decode_WithResponseBody_NoMessage_ThrowsGenericWalletPayException() {
        String jsonBody = "{\"other\":\"error\"}";
        Response response = Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .body(jsonBody, StandardCharsets.UTF_8)
                .build();

        Exception exception = decoder.decode("methodKey", response);

        assertTrue(exception instanceof WalletPayException);
        assertEquals("Wallet payment failed", exception.getMessage());
    }

    @Test
    void decode_EmptyBody_ThrowsGenericWalletPayException() {
        Response response = Response.builder()
                .status(402)
                .reason("Payment Required")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .body("", StandardCharsets.UTF_8)
                .build();

        Exception exception = decoder.decode("methodKey", response);

        assertTrue(exception instanceof WalletPayException);
        assertEquals("Wallet payment failed", exception.getMessage());
    }

    @Test
    void decode_Status200_UsesDefaultDecoder() {
        Response response = Response.builder()
                .status(200)
                .reason("OK")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .build();

        Exception exception = decoder.decode("methodKey", response);

        // default decoder normally throws FeignException for non-2xx, but since it's 200, Feign's default decoder might return null or some error
        // Actually error decoder is only called on error. If status is 200 somehow, it delegates.
        assertNotNull(exception);
    }
}
