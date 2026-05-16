package com.booknest.wallet_service.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

@Service
public class WalletRazorpayService {

    @Value("${razorpay.key:}")
    private String key;

    @Value("${razorpay.secret:}")
    private String secret;

    public String createOrderInr(double amountRupees, String receipt) throws Exception {
        if (key == null || key.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException("Razorpay is not configured");
        }
        RazorpayClient client = new RazorpayClient(key, secret);
        JSONObject options = new JSONObject();
        long amountPaise = Math.round(amountRupees * 100.0);
        options.put("amount", amountPaise);
        options.put("currency", "INR");
        options.put("receipt", receipt);
        Order order = client.orders.create(options);
        return order.get("id");
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

    public String getPublishableKey() {
        return key;
    }
}
