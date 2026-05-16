package com.booknest.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "test-secret-that-is-long-enough-for-hmac-sha-256";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        jwtUtil.init();
    }

    @Test
    void init_ShouldThrowExceptionIfSecretMissing() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "");
        assertThrows(IllegalStateException.class, util::init);
    }

    @Test
    void init_ShouldThrowExceptionIfSecretNull() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", null);
        assertThrows(IllegalStateException.class, util::init);
    }

    @Test
    void extractUserId_WithBlankStringId_ShouldReturnNull() {
        String token = Jwts.builder()
                .claim("userId", "  ")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_WithUnexpectedType_ShouldReturnNull() {
        String token = Jwts.builder()
                .claim("userId", true)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = Jwts.builder()
                .setSubject("test@test.com")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        String token = Jwts.builder()
                .setSubject("test@test.com")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void extractEmail_ShouldReturnSubject() {
        String token = Jwts.builder()
                .setSubject("test@test.com")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals("test@test.com", jwtUtil.extractEmail(token));
    }

    @Test
    void extractRole_ShouldReturnRoleClaim() {
        String token = Jwts.builder()
                .claim("role", "ROLE_ADMIN")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void extractUserId_WithNumericId_ShouldReturnLong() {
        String token = Jwts.builder()
                .claim("userId", 123)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals(123L, jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_WithStringId_ShouldReturnLong() {
        String token = Jwts.builder()
                .claim("userId", "456")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals(456L, jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_WithInvalidStringId_ShouldReturnNull() {
        String token = Jwts.builder()
                .claim("userId", "abc")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_WithMissingId_ShouldReturnNull() {
        String token = Jwts.builder()
                .setSubject("test@test.com") // Make sure payload is not empty
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertNull(jwtUtil.extractUserId(token));
    }
}
