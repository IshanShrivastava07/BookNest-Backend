package com.booknest.wishlist_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayFilterTest {

    @InjectMocks
    private GatewayFilter filter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @Test
    void doFilterInternal_SwaggerPath_ShouldForward() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidSecret_ShouldReturnForbidden() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "secret", "correct-secret");
        when(request.getRequestURI()).thenReturn("/wishlist");
        when(request.getHeader("X-Gateway-Secret")).thenReturn("wrong-secret");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void doFilterInternal_ValidSecret_ShouldForward() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "secret", "correct-secret");
        when(request.getRequestURI()).thenReturn("/wishlist");
        when(request.getHeader("X-Gateway-Secret")).thenReturn("correct-secret");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
