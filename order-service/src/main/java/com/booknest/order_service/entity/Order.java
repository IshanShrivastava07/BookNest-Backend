package com.booknest.order_service.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long userId;
    private double amountPaid;
    private String modeOfPayment;
    private String orderStatus;
    /** Total units across all line items */
    private int quantity;
    /** Number of distinct books (line items) */
    private int bookLineCount;

    private LocalDateTime orderDate;
    private LocalDate estimatedDeliveryDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "orderId")
    private java.util.List<OrderItem> items;
}
