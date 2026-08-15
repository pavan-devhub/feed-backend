package com.feedstartup.controller;

import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Match LoginController CORS config
public class AuthController {

    private final UserRepository userRepository;

    @Autowired
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "error", "Unauthorized"));
        }

        // We set the email as principal and userId as credentials in the JwtAuthenticationFilter
        String email = (String) authentication.getPrincipal();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "error", "User not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("userId", user.getId());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("userType", user.getUserType());

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/logout", produces = "application/json")
    public ResponseEntity<Map<String, Object>> logout() {
        // Since JWT is stateless, server-side invalidation is handled by the client dropping the token.
        // We just return a success message here to complete the API flow.
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(response);
    }
}
