package com.feedstartup.controller;

import com.feedstartup.dto.EpmGalleryDistrictDetailDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryStateDto;
import com.feedstartup.service.EpmGalleryRegionService;
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
 * Public endpoints backing the EPM page's and gallery page's images. Every image is uploaded
 * through the admin panel: each request looks the image up in the database first (name, block,
 * district) and only then streams the file that row names from disk. The /states routes serve the
 * state -> district -> photo tree (EpmGalleryRegionService).
 */
@RestController
@RequestMapping("/api/epm/gallery")
@CrossOrigin(origins = "*")
public class EpmGalleryController {

    private final EpmGalleryService epmGalleryService;
    private final EpmGalleryRegionService epmGalleryRegionService;

    @Autowired
    public EpmGalleryController(EpmGalleryService epmGalleryService, EpmGalleryRegionService epmGalleryRegionService) {
        this.epmGalleryService = epmGalleryService;
        this.epmGalleryRegionService = epmGalleryRegionService;
    }

    /** Every photo in the gallery page's sections - the EPM page's carousel falls back to these. */
    @GetMapping
    public ResponseEntity<List<EpmGalleryImageDto>> list() {
        return ResponseEntity.ok(epmGalleryService.list());
    }

    /** Just one section's images, e.g. "epm-moments" or "epm-hero" - see EpmGalleryBlock. */
    @GetMapping("/{block}")
    public ResponseEntity<List<EpmGalleryImageDto>> listByBlock(@PathVariable String block) {
        return ResponseEntity.ok(epmGalleryService.listByBlock(block));
    }

    @GetMapping("/{block}/{id}/file")
    public ResponseEntity<Resource> streamImage(@PathVariable String block, @PathVariable Long id) {
        return streamed(epmGalleryService.loadImageFile(block, id));
    }

    // --- state -> district -> photos -----------------------------------------------------------
    // "states" is a literal path segment, so it always wins over the /{block} routes above.

    /** The state cards for the top of the gallery page, each with its district cards. */
    @GetMapping("/states")
    public ResponseEntity<List<EpmGalleryStateDto>> listStates() {
        return ResponseEntity.ok(epmGalleryRegionService.listStates());
    }

    @GetMapping("/states/{state}")
    public ResponseEntity<EpmGalleryStateDto> getState(@PathVariable String state) {
        return ResponseEntity.ok(epmGalleryRegionService.getState(state));
    }

    @GetMapping("/states/{state}/districts/{district}")
    public ResponseEntity<EpmGalleryDistrictDetailDto> getDistrict(@PathVariable String state,
                                                                   @PathVariable String district) {
        return ResponseEntity.ok(epmGalleryRegionService.getDistrict(state, district));
    }

    @GetMapping("/states/{state}/cover")
    public ResponseEntity<Resource> streamStateCover(@PathVariable String state) {
        return streamed(epmGalleryRegionService.loadStateCover(state));
    }

    @GetMapping("/states/{state}/districts/{district}/{id}/file")
    public ResponseEntity<Resource> streamDistrictPhoto(@PathVariable String state, @PathVariable String district,
                                                        @PathVariable Long id) {
        return streamed(epmGalleryRegionService.loadPhoto(state, district, id));
    }

    private static ResponseEntity<Resource> streamed(StoredFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }
}
