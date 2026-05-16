package com.booknest.auth_service.service;

import com.booknest.auth_service.dto.RegisterRequest;

import com.booknest.auth_service.dto.UserResponse;

public interface AuthService {
	String register(RegisterRequest request);
    String login(String email, String password);
	String verifyOtp(String email, String otp);
    String resendOtp(String email);
    UserResponse getMe(String token);
}