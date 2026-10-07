package com.feedstartup.controller;

import com.feedstartup.dto.EpmAdminActivityDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.service.EpmAdminActivityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * The admin panel's "EPM Activity" log - which admin scheduled, changed, cancelled, reinstated or
 * deleted each EPM. Read-only: entries are written by EpmEventServiceImpl as admins work, and
 * there is no route to change or remove one. Gated behind the ADMIN role in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/epm/activity")
@CrossOrigin(origins = "*")
public class AdminEpmActivityController {

    private final EpmAdminActivityService activityService;

    public AdminEpmActivityController(EpmAdminActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * A page of entries, newest first. Optional filters: {@code adminId}, {@code action} (created,
     * updated, cancelled, reinstated or deleted) and {@code eventDate} (yyyy-MM-dd, the EPM's date).
     */
    @GetMapping
    public ResponseEntity<PageDto<EpmAdminActivityDto>> page(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activityService.page(adminId, action, eventDate, page, size));
    }

    /** The "Admin" filter's choices: everyone with an entry, deleted admins included. */
    @GetMapping("/admins")
    public ResponseEntity<List<EpmAdminActivityDto.Admin>> admins() {
        return ResponseEntity.ok(activityService.admins());
    }
}
