package com.feedstartup.controller;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.dto.UserNotificationsDto;
import com.feedstartup.service.EpmEventService;
import com.feedstartup.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public, read-only endpoints backing the EPM page's "Upcoming EPMs" list, the details/filter
 * screen, the location picker on the register/volunteer forms, and the notification bell of a
 * visitor who isn't logged in.
 */
@RestController
@RequestMapping("/api/epm/events")
@CrossOrigin(origins = "*")
public class EpmEventController {

    private final EpmEventService epmEventService;
    private final UserNotificationService userNotificationService;

    @Autowired
    public EpmEventController(EpmEventService epmEventService, UserNotificationService userNotificationService) {
        this.epmEventService = epmEventService;
        this.userNotificationService = userNotificationService;
    }

    /** @param includeCancelled also list upcoming EPMs that were cancelled (the register / volunteer pages mark them) */
    @GetMapping
    public ResponseEntity<List<EpmEventDto>> list(
            @RequestParam(defaultValue = "upcoming") String status,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") boolean includeCancelled) {
        return ResponseEntity.ok(epmEventService.list(status, state, district, city, category, month, year, includeCancelled));
    }

    @GetMapping("/stats")
    public ResponseEntity<EpmStatsDto> stats() {
        return ResponseEntity.ok(epmEventService.getStats());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<EpmCategoryDto>> categories() {
        return ResponseEntity.ok(epmEventService.listCategories());
    }

    /** New-EPM announcements (15 days before each EPM) for the bell of a visitor who isn't logged in. */
    @GetMapping("/announcements")
    public ResponseEntity<UserNotificationsDto> announcements() {
        return ResponseEntity.ok(userNotificationService.announcements());
    }

    // Constrained to digits so it doesn't swallow the /stats, /categories and /announcements routes above.
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<EpmEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(epmEventService.getById(id));
    }
}
