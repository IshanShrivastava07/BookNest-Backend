package com.booknest.auth_service.service;

import com.booknest.auth_service.dto.RegisterRequest;
import com.booknest.auth_service.entity.User;
import com.booknest.auth_service.messaging.AuthEventPublisher;
import com.booknest.auth_service.repository.UserRepository;
import com.booknest.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository repo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthEventPublisher authEventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User unverifiedUser;
    private User verifiedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setFullName("Test User");
        registerRequest.setPassword("password123");

        unverifiedUser = new User();
        unverifiedUser.setUserId(1L);
        unverifiedUser.setEmail("test@example.com");
        unverifiedUser.setFullName("Test User");
        unverifiedUser.setPassword("encodedPassword");
        unverifiedUser.setRole("ROLE_USER");
        unverifiedUser.setVerified(false);

        verifiedUser = new User();
        verifiedUser.setUserId(2L);
        verifiedUser.setEmail("verified@example.com");
        verifiedUser.setFullName("Verified User");
        verifiedUser.setPassword("encodedPassword");
        verifiedUser.setRole("ROLE_USER");
        verifiedUser.setVerified(true);
    }

    @Test
    void register_NewUser_ShouldSendOtp() {
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("password123")).thenReturn("encodedPassword");
        when(repo.save(any(User.class))).thenReturn(unverifiedUser);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String result = authService.register(registerRequest);

        assertEquals("OTP sent to email. Please verify.", result);
        verify(repo).save(any(User.class));
        verify(emailService).sendOtp(eq("test@example.com"), anyString());
    }

    @Test
    void register_AlreadyVerifiedUser_ShouldThrowException() {
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email is already registered and verified. Please login.", exception.getMessage());
    }

    @Test
    void verifyOtp_ValidRedisOtp_ShouldReturnJwtAndPublishEvent() {
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:otp:test@example.com")).thenReturn("123456");
        when(jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER")).thenReturn("jwt-token");

        String token = authService.verifyOtp("test@example.com", "123456");

        assertEquals("jwt-token", token);
        assertTrue(unverifiedUser.isVerified());
        assertNull(unverifiedUser.getOtp());
        verify(repo).save(unverifiedUser);
        verify(authEventPublisher).publishUserRegistered(1L, "test@example.com", "Test User");
    }

    @Test
    void verifyOtp_ValidDbOtp_ShouldReturnJwtAndPublishEvent() {
        unverifiedUser.setOtp("654321");
        unverifiedUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get("auth:otp:test@example.com")).thenReturn(null); // Redis misses
        when(jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER")).thenReturn("jwt-token");

        String token = authService.verifyOtp("test@example.com", "654321");

        assertEquals("jwt-token", token);
        assertTrue(unverifiedUser.isVerified());
        verify(repo).save(unverifiedUser);
    }

    @Test
    void verifyOtp_ExpiredDbOtp_ShouldThrowException() {
        unverifiedUser.setOtp("654321");
        unverifiedUser.setOtpExpiry(LocalDateTime.now().minusMinutes(1)); // Expired

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get("auth:otp:test@example.com")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyOtp("test@example.com", "654321");
        });

        assertEquals("OTP has expired. Please register again to get a new one.", exception.getMessage());
    }

    @Test
    void verifyOtp_InvalidOtp_ShouldThrowException() {
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get("auth:otp:test@example.com")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyOtp("test@example.com", "111111");
        });

        assertEquals("Invalid or expired OTP", exception.getMessage());
    }

    @Test
    void verifyOtp_RedisFallbackHandling_ShouldCheckDb() {
        unverifiedUser.setOtp("123123");
        unverifiedUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        // Simulate Redis failure
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));
        when(jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER")).thenReturn("jwt-token");

        String token = authService.verifyOtp("test@example.com", "123123");

        assertEquals("jwt-token", token);
        verify(repo).save(unverifiedUser);
    }

    @Test
    void login_ValidCredentials_ShouldReturnJwt() {
        when(repo.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken("verified@example.com", 2L, "ROLE_USER")).thenReturn("jwt-token");

        String token = authService.login("verified@example.com", "password123");

        assertEquals("jwt-token", token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_InvalidCredentials_ShouldThrowException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("verified@example.com", "wrongpassword");
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_UnverifiedUser_ShouldThrowException() {
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("test@example.com", "password123");
        });

        assertEquals("Please verify your email first", exception.getMessage());
    }

    @Test
    void resendOtp_UserFound_ShouldSendOtp() {
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String result = authService.resendOtp("test@example.com");

        assertEquals("OTP sent to email. Please verify.", result);
        verify(emailService).sendOtp(eq("test@example.com"), anyString());
    }

    @Test
    void resendOtp_UserNotFound_ShouldThrowException() {
        when(repo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resendOtp("unknown@example.com");
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void resendOtp_AlreadyVerified_ShouldThrowException() {
        when(repo.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resendOtp("verified@example.com");
        });

        assertEquals("Email is already verified. Please login.", exception.getMessage());
    }

    @Test
    void getMe_ValidToken_ShouldReturnUserResponse() {
        when(jwtUtil.extractEmail("valid-token")).thenReturn("verified@example.com");
        when(repo.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        com.booknest.auth_service.dto.UserResponse response = authService.getMe("valid-token");

        assertNotNull(response);
        assertEquals("verified@example.com", response.getEmail());
        assertEquals("Verified User", response.getFullName());
    }

    @Test
    void getMe_UserNotFound_ShouldThrowException() {
        when(jwtUtil.extractEmail("token-with-unknown-user")).thenReturn("unknown@example.com");
        when(repo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.getMe("token-with-unknown-user");
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void register_EmailServiceFailure_ShouldStillReturnSuccess() {
        // Covering the catch block in sendOtp
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("password123")).thenReturn("encodedPassword");
        doThrow(new RuntimeException("Email service down")).when(emailService).sendOtp(anyString(), anyString());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String result = authService.register(registerRequest);

        assertEquals("OTP sent to email. Please verify.", result);
    }

    @Test
    void cacheOtp_RedisFailure_ShouldNotThrowException() {
        // Testing private method indirectly through register
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));

        assertDoesNotThrow(() -> authService.register(registerRequest));
    }

    @Test
    void clearCachedOtp_RedisFailure_ShouldNotThrowException() {
        // Testing private method indirectly through verifyOtp
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> authService.verifyOtp("test@example.com", "123456"));
    }
}
