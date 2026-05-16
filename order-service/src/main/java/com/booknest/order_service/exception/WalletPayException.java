package com.booknest.order_service.exception;

public class WalletPayException extends RuntimeException {

    public WalletPayException(String message) {
        super(message);
    }
}
