package com.booknest.auth_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        // Secret must be at least 32 chars for HS256
        ReflectionTestUtils.setField(jwtUtil, "secret", "5367566B59703373367639792F423F4528482B4D");
        // Manually call @PostConstruct
        jwtUtil.init();
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String token = jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER");

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void extractClaims_ShouldReturnCorrectData() {
        String token = jwtUtil.generateToken("admin@example.com", 2L, "ROLE_ADMIN");

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        assertEquals("admin@example.com", email);
        assertEquals("ROLE_ADMIN", role);
        // userId is embedded in claims as Integer from JSON
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey((java.security.Key) ReflectionTestUtils.getField(jwtUtil, "key"))
                .build()
                .parseClaimsJws(token)
                .getBody();
        Number userId = (Number) claims.get("userId");
        assertEquals(2L, userId.longValue());
    }

    @Test
    void validateToken_ValidToken_ShouldReturnTrue() {
        String token = jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER");

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_InvalidToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_ExpiredToken_ShouldReturnFalse() {
        // Create an expired token manually
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 10000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 5000))
                .signWith((java.security.Key) ReflectionTestUtils.getField(jwtUtil, "key"), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void validateToken_MalformedToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("abc.def.ghi"));
    }

    @Test
    void validateToken_SignatureMismatch_ShouldReturnFalse() {
        String token = jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER");
        // Tamper with the token
        String tamperedToken = token + "tampered";
        assertFalse(jwtUtil.validateToken(tamperedToken));
    }

    @Test
    void init_EmptySecret_ShouldThrowException() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "");
        assertThrows(IllegalStateException.class, util::init);
    }

    @Test
    void init_NullSecret_ShouldThrowException() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", null);
        assertThrows(IllegalStateException.class, util::init);
    }
}
