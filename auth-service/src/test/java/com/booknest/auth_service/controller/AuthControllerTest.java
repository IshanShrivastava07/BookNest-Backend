package com.booknest.auth_service.controller;

import com.booknest.auth_service.dto.LoginRequest;
import com.booknest.auth_service.dto.RegisterRequest;
import com.booknest.auth_service.dto.UserResponse;
import com.booknest.auth_service.service.AuthService;
import com.booknest.auth_service.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller testing
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ValidRequest_ShouldReturnSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(authService.register(any(RegisterRequest.class))).thenReturn("OTP sent");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent"));
    }

    @Test
    void login_ValidRequest_ShouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(anyString(), anyString())).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void verifyOtp_ValidRequest_ShouldReturnToken() throws Exception {
        when(authService.verifyOtp(anyString(), anyString())).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/verify")
                .param("email", "test@example.com")
                .param("otp", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void resendOtp_ValidRequest_ShouldReturnSuccess() throws Exception {
        when(authService.resendOtp(anyString())).thenReturn("OTP resent");

        mockMvc.perform(post("/auth/resend-otp")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP resent"));
    }

    @Test
    void getMe_ValidHeader_ShouldReturnUser() throws Exception {
        UserResponse response = new UserResponse();
        response.setEmail("test@example.com");
        response.setFullName("Test User");

        when(authService.getMe("valid-token")).thenReturn(response);

        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getMe_InvalidHeader_ShouldReturnError() throws Exception {
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "InvalidToken"))
                .andExpect(status().isBadRequest());
    }
}
