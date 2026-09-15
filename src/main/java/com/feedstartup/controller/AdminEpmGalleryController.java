package com.feedstartup.controller;

import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.service.EpmGalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Uploads/edits/removes EPM gallery photos, scoped to one block (see EpmGalleryBlock) at a time.
 * Gated behind the same shared admin key as AdminPublicationController and AdminEpmEventController
 * - see AdminPublicationController's javadoc for why this is a header-key check rather than a
 * full admin role.
 */
@RestController
@RequestMapping("/api/admin/epm/gallery")
@CrossOrigin(origins = "*")
public class AdminEpmGalleryController {

    private final EpmGalleryService epmGalleryService;

    @Value("${feedworld.admin.upload-key}")
    private String adminUploadKey;

    @Autowired
    public AdminEpmGalleryController(EpmGalleryService epmGalleryService) {
        this.epmGalleryService = epmGalleryService;
    }

    @PostMapping(value = "/{block}", consumes = "multipart/form-data")
    public ResponseEntity<?> upload(
            @RequestHeader("X-Admin-Key") String providedKey,
            @PathVariable String block,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(required = false) Integer displayOrder) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        EpmGalleryImageDto created = epmGalleryService.upload(block, file, caption, city, state, featured, displayOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{block}/{id}")
    public ResponseEntity<?> update(
            @RequestHeader("X-Admin-Key") String providedKey,
            @PathVariable String block,
            @PathVariable String id,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Integer displayOrder) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        return ResponseEntity.ok(epmGalleryService.updateMetadata(block, id, caption, city, state, featured, displayOrder));
    }

    @DeleteMapping("/{block}/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("X-Admin-Key") String providedKey,
            @PathVariable String block,
            @PathVariable String id) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        epmGalleryService.delete(block, id);
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
