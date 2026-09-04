package com.feedstartup.controller;

import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.service.EpmEventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mutates the EPM event calendar (create/edit/remove a meeting), so it is gated behind the same
 * shared admin key as AdminPublicationController rather than a full admin role - see that
 * controller's javadoc for why.
 */
@RestController
@RequestMapping("/api/admin/epm/events")
@CrossOrigin(origins = "*")
public class AdminEpmEventController {

    private final EpmEventService epmEventService;

    @Value("${feedworld.admin.upload-key}")
    private String adminUploadKey;

    @Autowired
    public AdminEpmEventController(EpmEventService epmEventService) {
        this.epmEventService = epmEventService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@RequestHeader("X-Admin-Key") String providedKey,
                                     @Valid @RequestBody EpmEventRequestDto dto) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        EpmEventDto created = epmEventService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@RequestHeader("X-Admin-Key") String providedKey,
                                     @PathVariable Long id,
                                     @Valid @RequestBody EpmEventRequestDto dto) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        return ResponseEntity.ok(epmEventService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader("X-Admin-Key") String providedKey, @PathVariable Long id) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        epmEventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> checkAdminKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(adminUploadKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "error", "Invalid or missing X-Admin-Key header"));
        }
        return null;
    }
}
