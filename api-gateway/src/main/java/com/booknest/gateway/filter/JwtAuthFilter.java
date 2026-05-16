package com.booknest.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.booknest.gateway.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/books")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/orders/test-public");
    }

    private boolean isRoleProtectedPath(String path) {
        return path.startsWith("/cart/")
                || path.startsWith("/orders/")
                || path.startsWith("/wallet/")
                || path.startsWith("/wishlist/")
                || path.startsWith("/reviews/")
                || path.startsWith("/notifications/");
    }

    private boolean isAdminOnlyPath(String path, HttpMethod method) {
        if (path.startsWith("/admin/")) {
            return true;
        }
        // Catalog browse is public; write operations are admin-only.
        if (path.startsWith("/books") && method != HttpMethod.GET) {
            return true;
        }
        // Order status changes should be admin-only.
        return path.matches("^/orders/\\d+/status$") && method == HttpMethod.PUT;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isPublicPath(path) && !isAdminOnlyPath(path, method)) {
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Gateway-Secret", gatewaySecret)
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, "SOURCE: GATEWAY | Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "SOURCE: GATEWAY | Invalid Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return onError(exchange, "SOURCE: GATEWAY | Invalid Token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);
        Long userId = jwtUtil.extractUserId(token);
        log.info("Request processed for userId={} on path={}", userId, path);

        if (userId == null) {
            return onError(exchange, "SOURCE: GATEWAY | Missing userId in token", HttpStatus.UNAUTHORIZED);
        }

        if (role == null || role.isBlank()) {
            return onError(exchange, "SOURCE: GATEWAY | Missing role in token", HttpStatus.FORBIDDEN);
        }
        if (isRoleProtectedPath(path) && !("ROLE_USER".equals(role) || "ROLE_ADMIN".equals(role))) {
            return onError(exchange, "SOURCE: GATEWAY | Access denied for role", HttpStatus.FORBIDDEN);
        }
        if (isAdminOnlyPath(path, method) && !"ROLE_ADMIN".equals(role)) {
            return onError(exchange, "SOURCE: GATEWAY | Admin role required", HttpStatus.FORBIDDEN);
        }

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Email", email != null ? email : "")
                .header("X-User-Role", role != null ? role : "")
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        log.warn("Authentication failure: {} | Status: {} | Path: {}", err, status, exchange.getRequest().getURI().getPath());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + status.getReasonPhrase() + "\", \"message\":\"" + err + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
