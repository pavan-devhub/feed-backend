package com.feedstartup.controller;

import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.StoredFile;
import com.feedstartup.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Personal profile picture for the logged-in user, shown in the navbar account button in place
 * of their initial once set. Upload/removal require auth (JwtAuthenticationFilter sets the email
 * principal - see requireCurrentUserId); the file itself is streamed back through a public GET so
 * a plain <img> tag can load it without an Authorization header, same tradeoff as
 * EpmGalleryController.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserProfileController {

    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public UserProfileController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostMapping(value = "/me/profile-image", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        Long userId = requireCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "error", "Unauthorized"));
        }

        User updated = userService.updateProfileImage(userId, file);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "profileImageUrl", profileImageUrl(updated.getId())
        ));
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<?> removeProfileImage() {
        Long userId = requireCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "error", "Unauthorized"));
        }

        userService.removeProfileImage(userId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/{id}/profile-image/file")
    public ResponseEntity<Resource> streamProfileImage(@PathVariable Long id) {
        StoredFile file = userService.loadProfileImageFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(file.resource());
    }

    static String profileImageUrl(Long userId) {
        return "/api/users/" + userId + "/profile-image/file";
    }

    private Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String email = (String) authentication.getPrincipal();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
