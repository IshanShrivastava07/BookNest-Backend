package com.booknest.notification_service.controller;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationControllerTest {

    @Mock private NotificationService service;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private NotificationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "request", request);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByUser_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        mockMvc.perform(get("/notifications")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void send_Success() throws Exception {
        mockMvc.perform(post("/notifications")
                .contentType("application/json")
                .content("{\"userId\":10, \"message\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        mockMvc.perform(put("/notifications/1/read")
                .header("X-User-Id", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentUserId_Missing_ReturnsError() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> controller.listMine());
    }
}
