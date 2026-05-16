package com.booknest.auth_service.config;

import com.booknest.auth_service.entity.User;
import com.booknest.auth_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AdminSeeder {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "ishansh07@gmail.com";
            var userOpt = userRepository.findByEmail(adminEmail);
            if (userOpt.isEmpty()) {
                User admin = new User();
                admin.setFullName("System Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("Frustum7"));
                admin.setRole("ROLE_ADMIN");
                admin.setVerified(true);
                userRepository.save(admin);
                log.info("Admin user seeded successfully: {}", adminEmail);
            } else {
                User existingUser = userOpt.get();
                boolean updated = false;
                if (!"ROLE_ADMIN".equals(existingUser.getRole())) {
                    existingUser.setRole("ROLE_ADMIN");
                    updated = true;
                }
                // Also update password to ensure it matches the user's request
                existingUser.setPassword(passwordEncoder.encode("Frustum7"));
                existingUser.setVerified(true);
                userRepository.save(existingUser);
                log.info("Admin user updated: {}", adminEmail);
            }
        };
    }
}
