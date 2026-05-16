package com.booknest.gateway.filter;

import com.booknest.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtAuthFilter, "gatewaySecret", "test-secret");
        lenient().when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void filter_PublicPath_ShouldForwardWithGatewaySecret() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, filterChain).block();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(exchangeCaptor.capture());
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertEquals("test-secret", capturedExchange.getRequest().getHeaders().getFirst("X-Gateway-Secret"));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_OptionsRequest_ShouldForwardImmediately() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.OPTIONS, "/orders/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, filterChain).block();

        verify(filterChain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_MissingAuthHeader_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void filter_InvalidTokenFormat_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "InvalidTokenFormat")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void filter_InvalidToken_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    void filter_ValidToken_ShouldForwardWithHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("test@test.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("ROLE_USER");

        jwtAuthFilter.filter(exchange, filterChain).block();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(exchangeCaptor.capture());

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertEquals("test-secret", capturedExchange.getRequest().getHeaders().getFirst("X-Gateway-Secret"));
        assertEquals("1", capturedExchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("test@test.com", capturedExchange.getRequest().getHeaders().getFirst("X-User-Email"));
        assertEquals("ROLE_USER", capturedExchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void filter_AdminPathWithoutAdminRole_ShouldReturnForbidden() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
        when(jwtUtil.extractRole("valid-token")).thenReturn("ROLE_USER");

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }
    
    @Test
    void filter_AdminPathWithAdminRole_ShouldForward() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(2L);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("admin@test.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("ROLE_ADMIN");

        jwtAuthFilter.filter(exchange, filterChain).block();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(exchangeCaptor.capture());

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertEquals("ROLE_ADMIN", capturedExchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void filter_RoleProtectedPathWithValidRole_ShouldForward() {
        String[] protectedPaths = {"/cart/add", "/orders/history", "/wallet/balance", "/wishlist/view", "/reviews/post", "/notifications/all"};
        for (String path : protectedPaths) {
            MockServerHttpRequest request = MockServerHttpRequest.get(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            reset(jwtUtil, filterChain);
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
            when(jwtUtil.validateToken("valid-token")).thenReturn(true);
            when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
            when(jwtUtil.extractRole("valid-token")).thenReturn("ROLE_USER");

            jwtAuthFilter.filter(exchange, filterChain).block();

            verify(filterChain).filter(any(ServerWebExchange.class));
        }
    }

    @Test
    void filter_AdminPathSpecificRegex_ShouldRequireAdmin() {
        // PUT /orders/123/status
        MockServerHttpRequest request = MockServerHttpRequest.put("/orders/123/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("user-token")).thenReturn(true);
        when(jwtUtil.extractUserId("user-token")).thenReturn(1L);
        when(jwtUtil.extractRole("user-token")).thenReturn("ROLE_USER");

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_MissingUserIdInToken_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(null);

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_MissingRoleInToken_ShouldReturnForbidden() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
        when(jwtUtil.extractRole("valid-token")).thenReturn("");

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_PublicPaths_ShouldBypassAuth() {
        String[] publicPaths = {"/books", "/swagger-ui/index.html", "/v3/api-docs", "/orders/test-public", "/oauth2/callback"};
        for (String path : publicPaths) {
            MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            reset(filterChain);
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            jwtAuthFilter.filter(exchange, filterChain).block();

            verify(filterChain).filter(any(ServerWebExchange.class));
            verifyNoInteractions(jwtUtil);
        }
    }

    @Test
    void filter_AdminOnlyRegexPath_ShouldAllowAdmin() {
        MockServerHttpRequest request = MockServerHttpRequest.put("/orders/101/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("admin-token")).thenReturn(true);
        when(jwtUtil.extractUserId("admin-token")).thenReturn(2L);
        when(jwtUtil.extractRole("admin-token")).thenReturn("ROLE_ADMIN");

        jwtAuthFilter.filter(exchange, filterChain).block();

        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_AdminOnlyPath_ShouldAllowAdmin() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("admin-token")).thenReturn(true);
        when(jwtUtil.extractUserId("admin-token")).thenReturn(2L);
        when(jwtUtil.extractRole("admin-token")).thenReturn("ROLE_ADMIN");

        jwtAuthFilter.filter(exchange, filterChain).block();

        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_RoleProtectedPathWithoutRole_ShouldReturnForbidden() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
        when(jwtUtil.extractRole("valid-token")).thenReturn(null);

        jwtAuthFilter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }
}
