package com.booknest.order_service.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.booknest.order_service.exception.WalletPayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.codec.ErrorDecoder;

public class WalletFeignErrorDecoder implements ErrorDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.body() != null && response.status() >= 400) {
            try (InputStream is = response.body().asInputStream()) {
                byte[] buf = is.readAllBytes();
                if (buf.length > 0) {
                    JsonNode root = MAPPER.readTree(new String(buf, StandardCharsets.UTF_8));
                    if (root.hasNonNull("message")) {
                        return new WalletPayException(root.get("message").asText());
                    }
                }
            } catch (IOException ignored) {
            }
            return new WalletPayException("Wallet payment failed");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
