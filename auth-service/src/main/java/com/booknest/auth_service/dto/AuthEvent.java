package com.booknest.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {
    private Long userId;
    private String email;
    private String fullName;
    private String eventType; // e.g. USER_REGISTERED
    private String message;
}
