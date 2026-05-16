package com.booknest.auth_service.dto;

import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
}