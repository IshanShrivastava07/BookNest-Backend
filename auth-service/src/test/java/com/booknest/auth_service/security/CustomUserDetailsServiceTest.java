package com.booknest.auth_service.security;

import com.booknest.auth_service.entity.User;
import com.booknest.auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository repo;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_UserFound_ShouldReturnUserDetails() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole("ROLE_USER");

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_UserNotFound_ShouldThrowException() {
        when(repo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername("unknown@example.com");
        });
    }
}
