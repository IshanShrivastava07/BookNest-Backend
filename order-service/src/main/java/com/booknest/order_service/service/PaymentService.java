package com.booknest.order_service.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.booknest.order_service.dto.PaymentOrderResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

@Service
public class PaymentService {

    @Value("${razorpay.key:}")
    private String key;

    @Value("${razorpay.secret:}")
    private String secret;

    public PaymentOrderResponse createOrder(double amount) throws Exception {
        if (key == null || key.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay is not configured. Set environment variables RAZORPAY_KEY and RAZORPAY_SECRET (see application docs).");
        }

        RazorpayClient client = new RazorpayClient(key, secret);

        JSONObject options = new JSONObject();
        options.put("amount", (int) (amount * 100));
        options.put("currency", "INR");
        options.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = client.orders.create(options);

        return new PaymentOrderResponse(
                order.get("id"),
                order.get("amount"),
                order.get("currency"),
                key);
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) throws Exception {
        if (secret == null || secret.isBlank()) {
            return false;
        }
        JSONObject attrs = new JSONObject();
        attrs.put("razorpay_order_id", orderId);
        attrs.put("razorpay_payment_id", paymentId);
        attrs.put("razorpay_signature", signature);
        return Utils.verifyPaymentSignature(attrs, secret);
    }
}
