package com.booknest.review_service.controller;

import com.booknest.review_service.dto.ReviewBookResponse;
import com.booknest.review_service.dto.ReviewRequest;
import com.booknest.review_service.entity.Review;
import com.booknest.review_service.service.ReviewService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewControllerTest {

    @Mock private ReviewService service;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private ReviewController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "request", request);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void addReview_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(service.addReview(eq(10L), any(ReviewRequest.class))).thenReturn(new Review());

        mockMvc.perform(post("/reviews")
                .header("X-User-Id", "10")
                .contentType("application/json")
                .content("{\"bookId\":1, \"rating\":5, \"comment\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void addReview_MissingHeader_ReturnsBadRequest() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        mockMvc.perform(post("/reviews")
                .contentType("application/json")
                .content("{\"bookId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("SOURCE: REVIEW-SERVICE | Missing X-User-Id header"));
    }

    @Test
    void getCurrentUserRole_Test() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        when(service.updateReview(anyLong(), eq("ADMIN"), anyLong(), any())).thenReturn(new Review());

        mockMvc.perform(put("/reviews/1")
                .header("X-User-Id", "10")
                .header("X-User-Role", "ADMIN")
                .contentType("application/json")
                .content("{\"comment\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void addReview_ServiceThrows_ReturnsBadRequest() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(service.addReview(anyLong(), any())).thenThrow(new IllegalArgumentException("Invalid rating"));

        mockMvc.perform(post("/reviews")
                .header("X-User-Id", "10")
                .contentType("application/json")
                .content("{\"bookId\":1, \"rating\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid rating"));
    }

    @Test
    void getForBook_Success() throws Exception {
        when(service.getReviewsForBook(1L)).thenReturn(new ReviewBookResponse());

        mockMvc.perform(get("/reviews/book/1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReview_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(service.updateReview(anyLong(), anyString(), anyLong(), any())).thenReturn(new Review());

        mockMvc.perform(put("/reviews/1")
                .header("X-User-Id", "10")
                .header("X-User-Role", "ROLE_USER")
                .contentType("application/json")
                .content("{\"comment\":\"updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReview_Success() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("10");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");

        mockMvc.perform(delete("/reviews/1")
                .header("X-User-Id", "10")
                .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));
    }
}
