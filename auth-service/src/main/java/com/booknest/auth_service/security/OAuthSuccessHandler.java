package com.booknest.auth_service.security;
 
 import jakarta.servlet.ServletException;
 import jakarta.servlet.http.*;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.oauth2.core.user.*;
 import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
 import org.springframework.stereotype.Component;
 
 import java.io.IOException;
 import java.util.UUID;
 
 import com.booknest.auth_service.entity.User;
 import com.booknest.auth_service.repository.UserRepository;
 
 @Component
 public class OAuthSuccessHandler implements AuthenticationSuccessHandler {
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;
 
     @Autowired
     private UserRepository repo;
 
     @Autowired
     private JwtUtil jwtUtil;
 
     @Override
     public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication)
             throws IOException, ServletException {
 
         OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
 
         String email = oauthUser.getAttribute("email");
         String name = oauthUser.getAttribute("name");
 
         User user = repo.findByEmail(email).orElseGet(() -> {
             User newUser = new User();
             newUser.setEmail(email);
             newUser.setFullName(name);
             newUser.setPassword(UUID.randomUUID().toString()); // Random password for OAuth users
             newUser.setRole("ROLE_USER");
             newUser.setVerified(true);
             return repo.save(newUser);
         });
 
         String token = jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole());
 
         // Redirect to frontend with token
         response.sendRedirect(frontendUrl + "/oauth-success?token=" + token);
     }
 }