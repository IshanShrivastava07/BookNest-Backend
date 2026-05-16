package com.booknest.wallet_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayFilterTest {

    @InjectMocks
    private GatewayFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "secret", "test-secret");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void doFilterInternal_WithValidSecret_ShouldProceed() throws ServletException, IOException {
        request.setRequestURI("/wallet/1");
        request.addHeader("X-Gateway-Secret", "test-secret");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidSecret_ShouldReturnForbidden() throws ServletException, IOException {
        request.setRequestURI("/wallet/1");
        request.addHeader("X-Gateway-Secret", "wrong-secret");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertEquals("{\"message\":\"Access denied: requests must go through the API gateway\"}", response.getContentAsString());
    }
}
