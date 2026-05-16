package com.booknest.auth_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booknest.auth_service.dto.UserResponse;
import com.booknest.auth_service.repository.UserRepository;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public List<UserResponse> users() {
        return userRepository.findAll().stream().map(u -> {
            UserResponse r = new UserResponse();
            r.setUserId(u.getUserId());
            r.setFullName(u.getFullName());
            r.setEmail(u.getEmail());
            r.setRole(u.getRole());
            return r;
        }).toList();
    }
}
