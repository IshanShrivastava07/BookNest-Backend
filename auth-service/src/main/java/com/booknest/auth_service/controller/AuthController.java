package com.booknest.auth_service.controller;
 
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.web.bind.annotation.*;
 
 import com.booknest.auth_service.dto.AuthResponse;
 import com.booknest.auth_service.dto.LoginRequest;
 import com.booknest.auth_service.dto.RegisterRequest;
 import com.booknest.auth_service.dto.UserResponse;
 import com.booknest.auth_service.service.AuthService;
 
 import jakarta.validation.Valid;
 
 @RestController
 @RequestMapping("/auth")
 public class AuthController {
 	
     @Autowired
     private AuthService service;
 
     @PostMapping("/register")
     public String register(@Valid @RequestBody RegisterRequest request) {
         return service.register(request);
     }
 
     @PostMapping("/login")
     public AuthResponse login(@Valid @RequestBody LoginRequest request) {
         String token = service.login(request.getEmail(), request.getPassword());
         return new AuthResponse(token);
     }
 
    @PostMapping("/verify")
    public AuthResponse verifyOtp(@RequestParam String email,
                            @RequestParam String otp) {
        String token = service.verifyOtp(email, otp);
        return new AuthResponse(token);
    }
    
    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email) {
        return service.resendOtp(email);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {
        return service.requestPasswordReset(email);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody com.booknest.auth_service.dto.ResetPasswordRequest request) {
        return service.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
    }
     
     @GetMapping("/me")
     public UserResponse getMe(@RequestHeader("Authorization") String authHeader) {
         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
             throw new RuntimeException("Missing or invalid Authorization header");
         }
         String token = authHeader.substring(7);
         return service.getMe(token);
     }
 
     @GetMapping("/user/test")
     @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
     public String userApi() {
         return "Authorized: User Access Successful";
     }
 
     @GetMapping("/admin/test")
     @PreAuthorize("hasRole('ADMIN')")
     public String adminApi() {
         return "Authorized: Admin Access Successful";
     }
 }
