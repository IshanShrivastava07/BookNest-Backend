package com.booknest.gateway.util;
 
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
 
import java.nio.charset.StandardCharsets;
 import java.security.Key;
 
 @Component
 public class JwtUtil {
 
    @Value("${security.jwt.secret}")
    private String secret;
 
    private Key key;

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be configured");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
 
     public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }

    public Long extractUserId(String token) {
        Object raw = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("userId");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
 
     public boolean validateToken(String token) {
         try {
             Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
             return true;
         } catch (JwtException | IllegalArgumentException e) {
             return false;
         }
     }
 }
