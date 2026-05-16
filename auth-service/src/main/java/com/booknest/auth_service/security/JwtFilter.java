package com.booknest.auth_service.security;
 
 import jakarta.servlet.FilterChain;
 import jakarta.servlet.ServletException;
 import jakarta.servlet.http.HttpServletRequest;
 import jakarta.servlet.http.HttpServletResponse;
 
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
 import org.springframework.security.core.authority.SimpleGrantedAuthority;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
 import org.springframework.stereotype.Component;
 import org.springframework.web.filter.OncePerRequestFilter;
 
 import java.io.IOException;
 import java.util.Collections;
 
 @Component
 public class JwtFilter extends OncePerRequestFilter {
 
     @Autowired
     private JwtUtil jwtUtil;
 
     @Override
     protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
             throws ServletException, IOException {
 
         String path = request.getServletPath();
         String authHeader = request.getHeader("Authorization");
 
         // 1. Skip filtering for public auth endpoints
        if (path.startsWith("/auth/login") ||
            path.startsWith("/auth/register") ||
            path.startsWith("/auth/verify") ||
            path.startsWith("/auth/resend-otp") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs")) {
             filterChain.doFilter(request, response);
             return;
         }
 
         // 2. If no Bearer token, continue to next filter (SecurityConfig handles access control)
         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
             filterChain.doFilter(request, response);
             return;
         }
 
         String jwt = authHeader.substring(7);
         
         try {
             String userEmail = jwtUtil.extractEmail(jwt);
 
             if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                 if (jwtUtil.validateToken(jwt)) {
                     String role = jwtUtil.extractRole(jwt);
                     
                     UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                             userEmail,
                             null,
                             Collections.singletonList(new SimpleGrantedAuthority(role))
                     );
                     
                     authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                     SecurityContextHolder.getContext().setAuthentication(authToken);
                 }
             }
         } catch (Exception e) {
             // Silently ignore invalid tokens for now, SecurityConfig will block if authentication is required
         }
         
         filterChain.doFilter(request, response);
     }
 }