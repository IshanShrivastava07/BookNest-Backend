package com.booknest.order_service.service;

import java.util.List;

import com.booknest.order_service.dto.OrderRequest;
import com.booknest.order_service.entity.Order;

public interface OrderService {
    Order placeOrder(OrderRequest request);
    List<Order> getOrdersByUser(Long userId);
    Order getOrderById(Long orderId, Long userId);
    boolean userHasPurchasedBook(Long userId, Long bookId);
    Order updateOrderStatus(Long orderId, String status);
}
