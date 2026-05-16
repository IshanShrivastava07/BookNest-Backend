package com.booknest.order_service.entity;
 
 import jakarta.persistence.*;
 import lombok.AllArgsConstructor;
 import lombok.Data;
 import lombok.NoArgsConstructor;
 
 @Entity
 @Data
 @NoArgsConstructor
 @AllArgsConstructor
 @Table(name = "order_items")
 public class OrderItem {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long orderItemId;
 
    @Column(name = "orderId", insertable = false, updatable = false)
    private Long orderId; // Link to parent order
     private Long bookId;
     private String bookTitle;
     private String author;
     private String coverImage;
     private double price;
     private int quantity;
 }
