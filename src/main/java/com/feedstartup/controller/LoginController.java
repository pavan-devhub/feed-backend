package com.feedstartup.controller;

import com.feedstartup.dto.LoginDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.exception.TooManySessionsException;
import com.feedstartup.model.User;
import com.feedstartup.model.UserSession;
import com.feedstartup.service.SessionService;
import com.feedstartup.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.feedstartup.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow any frontend URL for development, or specify http://localhost:5173
public class LoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;

    @Autowired
    public LoginController(UserService userService, JwtUtil jwtUtil, SessionService sessionService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.sessionService = sessionService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> loginUser(@Valid @RequestBody LoginDto loginDto, HttpServletRequest request) {

        try {
            User user = userService.loginUser(loginDto);

            // One row per device - this both enforces the concurrent-device cap and gives us a jti
            // we can later invalidate on logout without waiting for the JWT to expire.
            UserSession session = sessionService.createSession(
                    user, request.getHeader("User-Agent"), request.getRemoteAddr());

            String token = jwtUtil.generateToken(user, session.getJti());

            // Return success JSON response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("userType", user.getUserType());
            response.put("profileImageUrl", user.getProfileImagePath() != null
                    ? UserProfileController.profileImageUrl(user.getId()) : null);

            return ResponseEntity.ok(response);

        } catch (TooManySessionsException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("maxSessions", 3);
            errorResponse.put("activeSessions", e.getActiveSessions());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (IllegalArgumentException e) {
            // Return failure JSON response for known issues (user not found / wrong password)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());

            HttpStatus status = "User not found".equals(e.getMessage())
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.UNAUTHORIZED;

            return ResponseEntity.status(status).body(errorResponse);
        } catch (Exception e) {
            // Return failure JSON response for server errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", "An internal error occurred during login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Called from the "too many devices" screen shown after a 409 on /login. The device
    // attempting to log in has no session/JWT of its own yet, so we can't use the normal
    // authenticated /api/auth/sessions/{id} revoke endpoint here - instead we re-verify the
    // user's password as proof of ownership before killing one of their other sessions.
    @PostMapping(value = "/login/sessions/{sessionId}/revoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> revokeSessionForLogin(@PathVariable Long sessionId,
                                                                       @Valid @RequestBody LoginDto loginDto) {
        try {
            User user = userService.loginUser(loginDto);
            sessionService.revokeSession(user.getId(), sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Logged out from that device");
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());

            HttpStatus status = "User not found".equals(e.getMessage())
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.UNAUTHORIZED;

            return ResponseEntity.status(status).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", "An internal error occurred while logging out that device");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
