package com.feedstartup.controller;

import com.feedstartup.dto.ActiveSessionDto;
import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.security.JwtUtil;
import com.feedstartup.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Match LoginController CORS config
public class AuthController {

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserRepository userRepository, SessionService sessionService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.jwtUtil = jwtUtil;
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
        response.put("profileImageUrl", user.getProfileImagePath() != null
                ? UserProfileController.profileImageUrl(user.getId()) : null);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/logout", produces = "application/json")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        // Removing the UserSession row invalidates this device's token immediately, even though
        // the JWT itself remains cryptographically valid until its long-lived expiry.
        String jwt = parseJwt(request);
        if (jwt != null) {
            String jti = jwtUtil.extractJti(jwt);
            sessionService.deleteByJti(jti);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logged out successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/sessions", produces = "application/json")
    public ResponseEntity<?> listSessions(HttpServletRequest request) {
        Long userId = requireCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "error", "Unauthorized"));
        }

        String currentJti = null;
        String jwt = parseJwt(request);
        if (jwt != null) {
            currentJti = jwtUtil.extractJti(jwt);
        }

        List<ActiveSessionDto> sessions = sessionService.listSessions(userId, currentJti);
        return ResponseEntity.ok(Map.of("status", "success", "sessions", sessions));
    }

    @DeleteMapping(value = "/sessions/{id}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> revokeSession(@PathVariable Long id) {
        Long userId = requireCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "error", "Unauthorized"));
        }

        sessionService.revokeSession(userId, id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Session revoked"));
    }

    private Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String email = (String) authentication.getPrincipal();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
