package com.feedstartup.controller;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.service.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mutates the publication catalog (upload, edit metadata, replace PDF, delete), so the whole
 * controller is gated behind the ADMIN role - see SecurityConfig's hasRole("ADMIN") matcher on
 * /api/admin/publications/**. Unlike the other Admin* controllers this uses per-user JWT auth
 * instead of the shared X-Admin-Key header.
 */
@RestController
@RequestMapping("/api/admin/publications")
@CrossOrigin(origins = "*")
public class AdminPublicationController {

    private final PublicationService publicationService;

    @Autowired
    public AdminPublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "English") PublicationLanguage language,
            @RequestParam(required = false) Integer volume,
            @RequestParam(required = false) Integer issueNumber) {
        PublicationDetailDto created = publicationService.uploadPublication(file, title, year, month, language, volume, issueNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer volume,
            @RequestParam(required = false) Integer issueNumber) {
        return ResponseEntity.ok(publicationService.updateMetadata(id, title, volume, issueNumber));
    }

    @PutMapping(value = "/{id}/pdf", consumes = "multipart/form-data")
    public ResponseEntity<?> replacePdf(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(publicationService.replacePdf(id, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        publicationService.deletePublication(id);
        return ResponseEntity.noContent().build();
    }
}
