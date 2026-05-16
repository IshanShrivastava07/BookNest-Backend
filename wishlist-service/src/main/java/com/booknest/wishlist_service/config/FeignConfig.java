package com.booknest.wishlist_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            template.header("Authorization", authHeader);
        }
        String gatewaySecret = request.getHeader("X-Gateway-Secret");
        if (gatewaySecret != null) {
            template.header("X-Gateway-Secret", gatewaySecret);
        }
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            template.header("X-User-Id", userId);
        }
        String userEmail = request.getHeader("X-User-Email");
        if (userEmail != null) {
            template.header("X-User-Email", userEmail);
        }
        String userRole = request.getHeader("X-User-Role");
        if (userRole != null) {
            template.header("X-User-Role", userRole);
        }
    }
}
