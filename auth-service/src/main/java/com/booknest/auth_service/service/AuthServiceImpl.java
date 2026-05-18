package com.booknest.auth_service.service;
 
 import java.time.LocalDateTime;
 import java.util.Random;
 
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.security.authentication.AuthenticationManager;
 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.stereotype.Service;
 import org.springframework.data.redis.core.StringRedisTemplate;
 
 import com.booknest.auth_service.dto.RegisterRequest;
 import com.booknest.auth_service.entity.User;
 import com.booknest.auth_service.messaging.AuthEventPublisher;
 import com.booknest.auth_service.repository.UserRepository;
 import com.booknest.auth_service.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
 
 @Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int OTP_EXPIRY_MINUTES = 5; // Enforced 5 minutes expiry
 
     @Autowired
     private UserRepository repo;
 
     @Autowired
     private PasswordEncoder encoder;
     
     @Autowired
     private JwtUtil jwtUtil;
     
     @Autowired
     private EmailService emailService;
     
     @Autowired
     private StringRedisTemplate redisTemplate;
 
     @Autowired
     private AuthenticationManager authenticationManager;

     @Autowired
     private AuthEventPublisher authEventPublisher;
 
     @Override
     public String register(RegisterRequest request) {
         String normalizedEmail = request.getEmail().trim().toLowerCase();
         
         User user = repo.findByEmail(normalizedEmail).orElse(new User());
         
         if (user.isVerified()) {
             throw new RuntimeException("Email is already registered and verified. Please login.");
         }
 
         // Update or create user
         user.setEmail(normalizedEmail);
         user.setFullName(request.getFullName().trim());
         user.setPassword(encoder.encode(request.getPassword()));
         user.setRole(DEFAULT_ROLE);
         user.setVerified(false);
         
         return sendOtp(user);
     }
 
    private String sendOtp(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        
        repo.save(user);
        cacheOtp(user.getEmail(), otp);

        log.info("Generated OTP for {}: {}", user.getEmail(), otp);

        try {
            emailService.sendOtp(user.getEmail(), otp);
            log.info("OTP sent to email: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP to email: {}. Error: {}", user.getEmail(), e.getMessage());
        }

        return "OTP sent to email. Please verify.";
    }

    @Override
    public String resendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (user.isVerified()) {
            throw new RuntimeException("Email is already verified. Please login.");
        }
        
        return sendOtp(user);
    }
 
     @Override
     public String verifyOtp(String email, String otp) {
         String normalizedEmail = email.trim().toLowerCase();
         User user = repo.findByEmail(normalizedEmail)
                 .orElseThrow(() -> new RuntimeException("User not found"));
 
         boolean isValid = false;
         String cachedOtp = getCachedOtp(normalizedEmail);
         
         if (cachedOtp != null && cachedOtp.equals(otp.trim())) {
             isValid = true;
         } else if (user.getOtp() != null && user.getOtp().equals(otp.trim())) {
             // DB Fallback
             if (user.getOtpExpiry() != null && user.getOtpExpiry().isBefore(LocalDateTime.now())) {
                 throw new RuntimeException("OTP has expired. Please register again to get a new one.");
             }
             isValid = true;
         }
         
         if (!isValid) {
            log.warn("Invalid OTP attempt for email: {}", normalizedEmail);
            throw new RuntimeException("Invalid or expired OTP");
        }
 
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        repo.save(user);
        clearCachedOtp(normalizedEmail);
 
        // Publish event safely
        authEventPublisher.publishUserRegistered(user.getUserId(), user.getEmail(), user.getFullName());

         return jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole());
     }
 
     @Override
     public String login(String email, String password) {
         String normalizedEmail = email.trim().toLowerCase();
         
         try {
             authenticationManager.authenticate(
                     new UsernamePasswordAuthenticationToken(normalizedEmail, password)
             );
         } catch (Exception e) {
            log.warn("Login failed for email: {}", normalizedEmail);
            throw new RuntimeException("Invalid email or password");
        }
 
         User user = repo.findByEmail(normalizedEmail)
                 .orElseThrow(() -> new RuntimeException("User not found"));
 
         if (!user.isVerified()) {
             throw new RuntimeException("Please verify your email first");
         }
 
         return jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole());
     }
 
     @Override
     public com.booknest.auth_service.dto.UserResponse getMe(String token) {
         String email = jwtUtil.extractEmail(token);
         User user = repo.findByEmail(email)
                 .orElseThrow(() -> new RuntimeException("User not found"));
         
         com.booknest.auth_service.dto.UserResponse response = new com.booknest.auth_service.dto.UserResponse();
         response.setUserId(user.getUserId());
         response.setFullName(user.getFullName());
         response.setEmail(user.getEmail());
         response.setRole(user.getRole());
         return response;
     }
 
    @Override
    public String requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + normalizedEmail));
        
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        
        repo.save(user);
        cacheOtp(user.getEmail(), otp);

        log.info("Generated Password Reset OTP for {}: {}", user.getEmail(), otp);

        try {
            emailService.sendPasswordResetOtp(user.getEmail(), otp);
            log.info("Password reset OTP sent to email: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to email: {}. Error: {}", user.getEmail(), e.getMessage());
        }

        return "Password reset OTP sent to email.";
    }

    @Override
    public String resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + normalizedEmail));

        boolean isValid = false;
        String cachedOtp = getCachedOtp(normalizedEmail);
        
        if (cachedOtp != null && cachedOtp.equals(otp.trim())) {
            isValid = true;
        } else if (user.getOtp() != null && user.getOtp().equals(otp.trim())) {
            // DB Fallback
            if (user.getOtpExpiry() != null && user.getOtpExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("OTP has expired. Please request a new password reset.");
            }
            isValid = true;
        }
        
        if (!isValid) {
            log.warn("Invalid password reset OTP attempt for email: {}", normalizedEmail);
            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setOtp(null);
        user.setOtpExpiry(null);
        repo.save(user);
        clearCachedOtp(normalizedEmail);

        log.info("Password successfully reset for email: {}", normalizedEmail);
        return "Password has been successfully reset.";
    }
 
 
     private void cacheOtp(String email, String otp) {
         try {
             if (redisTemplate != null) {
                 redisTemplate.opsForValue().set("auth:otp:" + email, otp, java.time.Duration.ofMinutes(OTP_EXPIRY_MINUTES));
             }
         } catch (Exception e) {
            log.warn("Redis connection failed, skipping cache for email {}: {}", email, e.getMessage());
        }
     }
 
     private String getCachedOtp(String email) {
         try {
             return redisTemplate != null ? redisTemplate.opsForValue().get("auth:otp:" + email) : null;
         } catch (Exception e) {
             return null;
         }
     }
 
     private void clearCachedOtp(String email) {
         try {
             if (redisTemplate != null) {
                 redisTemplate.delete("auth:otp:" + email);
             }
         } catch (Exception e) {
             // Ignore
         }
     }
 }