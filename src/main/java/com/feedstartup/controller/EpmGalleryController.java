package com.feedstartup.controller;

import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.service.EpmGalleryService;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public endpoints backing the EPM gallery page. Every photo is admin-uploaded and streamed from
 * disk through this controller (see EpmGalleryServiceImpl) instead of pointing the page at
 * third-party stock photo URLs.
 */
@RestController
@RequestMapping("/api/epm/gallery")
@CrossOrigin(origins = "*")
public class EpmGalleryController {

    private final EpmGalleryService epmGalleryService;

    @Autowired
    public EpmGalleryController(EpmGalleryService epmGalleryService) {
        this.epmGalleryService = epmGalleryService;
    }

    @GetMapping
    public ResponseEntity<List<EpmGalleryImageDto>> list() {
        return ResponseEntity.ok(epmGalleryService.list());
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> streamImage(@PathVariable Long id) {
        StoredFile file = epmGalleryService.loadImageFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }
}
