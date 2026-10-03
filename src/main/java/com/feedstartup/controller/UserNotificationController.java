package com.feedstartup.controller;

import com.feedstartup.dto.UserNotificationsDto;
import com.feedstartup.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * The logged-in user's notifications: their EPM sign-ups, the admin's updates to those EPMs, newly
 * opened EPMs and new Feed World issues. Requires a login (SecurityConfig's /api/users/me/** rule).
 */
@RestController
@RequestMapping("/api/users/me/notifications")
@CrossOrigin(origins = "*")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @Autowired
    public UserNotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @GetMapping
    public ResponseEntity<UserNotificationsDto> mine(Authentication authentication) {
        // JwtAuthenticationFilter sets the account's email as the principal.
        return ResponseEntity.ok(userNotificationService.forUser((String) authentication.getPrincipal()));
    }

    /** Called when the user opens the notifications panel - clears the unread count. */
    @PostMapping("/seen")
    public ResponseEntity<Void> markSeen(Authentication authentication) {
        userNotificationService.markAllSeen((String) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
