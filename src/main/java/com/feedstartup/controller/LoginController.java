package com.feedstartup.controller;

import com.feedstartup.dto.LoginDto;
import com.feedstartup.model.User;
import com.feedstartup.service.UserService;
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

    @Autowired
    public LoginController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> loginUser(@Valid @RequestBody LoginDto loginDto) {

        try {
            User user = userService.loginUser(loginDto);
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user);

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

            return ResponseEntity.ok(response);

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
}
