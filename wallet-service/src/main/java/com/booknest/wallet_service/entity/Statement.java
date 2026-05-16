package com.booknest.wallet_service.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long statementId;

    private Long walletId;

    /** Denormalized for statements API; may be null on legacy rows. */
    private Long userId;

    /** CREDIT or DEBIT */
    private String type;

    private double amount;

    @JsonProperty("timestamp")
    private LocalDateTime dateTime;

    @Column(name = "remarks")
    @JsonProperty("description")
    private String description;
}
