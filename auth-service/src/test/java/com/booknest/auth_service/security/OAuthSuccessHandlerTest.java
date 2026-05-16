package com.booknest.auth_service.security;

import com.booknest.auth_service.entity.User;
import com.booknest.auth_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthSuccessHandlerTest {

    @Mock
    private UserRepository repo;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuthSuccessHandler handler;

    @Test
    void onAuthenticationSuccess_ExistingUser_ShouldRedirectWithToken() throws Exception {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173");
        User user = new User();
        user.setEmail("test@example.com");
        user.setUserId(1L);
        user.setRole("ROLE_USER");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("test@example.com", 1L, "ROLE_USER")).thenReturn("jwt-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:5173/oauth-success?token=jwt-token");
    }

    @Test
    void onAuthenticationSuccess_NewUser_ShouldCreateUserAndRedirect() throws Exception {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173");
        User savedUser = new User();
        savedUser.setEmail("new@example.com");
        savedUser.setUserId(2L);
        savedUser.setRole("ROLE_USER");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("new@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("New User");
        when(repo.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repo.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("new@example.com", 2L, "ROLE_USER")).thenReturn("jwt-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(repo).save(any(User.class));
        verify(response).sendRedirect("http://localhost:5173/oauth-success?token=jwt-token");
    }
}
