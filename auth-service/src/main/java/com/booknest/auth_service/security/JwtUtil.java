package com.booknest.auth_service.security;
 
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
 
import java.nio.charset.StandardCharsets;
import java.security.Key;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.function.Function;
 
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
 
     public String generateToken(String email, Long userId, String role) {
         Map<String, Object> claims = new HashMap<>();
         claims.put("userId", userId);
         claims.put("role", role);
         return createToken(claims, email);
     }
 
     private String createToken(Map<String, Object> claims, String subject) {
         return Jwts.builder()
                 .setClaims(claims)
                 .setSubject(subject)
                 .setIssuedAt(new Date(System.currentTimeMillis()))
                 .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                 .signWith(key, SignatureAlgorithm.HS256)
                 .compact();
     }
 
     public String extractEmail(String token) {
         return extractClaim(token, Claims::getSubject);
     }
 
     public String extractRole(String token) {
         final Claims claims = extractAllClaims(token);
         return (String) claims.get("role");
     }
 
     public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
         final Claims claims = extractAllClaims(token);
         return claimsResolver.apply(claims);
     }
 
     private Claims extractAllClaims(String token) {
         return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
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
