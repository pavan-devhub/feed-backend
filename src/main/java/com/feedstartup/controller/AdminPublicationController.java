package com.feedstartup.controller;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.service.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Everything here mutates the publication catalog (upload a new issue, edit its metadata,
 * remove it), so it is gated behind a shared admin key rather than being open like the
 * read-only PublicationController endpoints.
 *
 * This is intentionally a lightweight header-key check rather than a full admin role, since
 * the existing User/JWT model in this project has no admin role yet. Swap this out for
 * proper role-based auth (e.g. a `role` column on User + hasRole("ADMIN")) if/when this needs
 * to be driven from a real admin UI instead of a script/Postman.
 */
@RestController
@RequestMapping("/api/admin/publications")
@CrossOrigin(origins = "*")
public class AdminPublicationController {

    private final PublicationService publicationService;

    @Value("${feedworld.admin.upload-key}")
    private String adminUploadKey;

    @Autowired
    public AdminPublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> upload(
            @RequestHeader("X-Admin-Key") String providedKey,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Integer volume,
            @RequestParam(required = false) Integer issueNumber) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        PublicationDetailDto created = publicationService.uploadPublication(file, title, year, month, volume, issueNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @RequestHeader("X-Admin-Key") String providedKey,
            @PathVariable String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer volume,
            @RequestParam(required = false) Integer issueNumber) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        return ResponseEntity.ok(publicationService.updateMetadata(id, title, volume, issueNumber));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader("X-Admin-Key") String providedKey, @PathVariable String id) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        publicationService.deletePublication(id);
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
