package com.booknest.wallet_service.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GatewayFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // ✅ Allow Swagger & public endpoints
        if (path.contains("/swagger-ui") ||
            path.contains("/v3/api-docs") ||
            path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔐 Validate Gateway Secret
        String header = request.getHeader("X-Gateway-Secret");

        if (header == null || !header.equals(secret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Access denied: requests must go through the API gateway\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}