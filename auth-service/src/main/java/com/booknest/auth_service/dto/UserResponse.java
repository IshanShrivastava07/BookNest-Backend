package com.booknest.auth_service.dto;

import lombok.Data;
@Data
public class UserResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String password;
    private String role;
}