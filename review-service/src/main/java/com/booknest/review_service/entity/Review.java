package com.booknest.review_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private Long bookId;
    private Long userId;

    private int rating; // 1–5
    private String comment;
    private LocalDateTime reviewDate;
}