package com.feedstartup.controller;

import com.feedstartup.dto.AdminPublicationRowDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.service.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lists and mutates the publication catalog (upload, replace PDF, delete), so the whole
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

    /**
     * The dashboard's publication table: a year's uploaded editions, a page at a time - see
     * PublicationService#pageForAdmin. {@code page} is 0-based and {@code size} is capped at 100;
     * the default 12 is four months of three editions.
     */
    @GetMapping
    public ResponseEntity<PageDto<AdminPublicationRowDto>> list(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) AdminPublicationRowDto.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(publicationService.pageForAdmin(year, month, status, page, size));
    }

    /** {@code title} is optional - left out or blank, the issue is titled "Feed World". */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "English") PublicationLanguage language,
            @RequestParam(required = false) String title) {
        PublicationDetailDto created = publicationService.uploadPublication(file, year, month, language, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
