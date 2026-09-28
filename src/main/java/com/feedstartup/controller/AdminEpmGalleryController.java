package com.feedstartup.controller;

import com.feedstartup.dto.EpmGalleryBlockDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryImageUpdateDto;
import com.feedstartup.service.EpmGalleryImportService;
import com.feedstartup.service.EpmGalleryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Uploads/edits/removes the images of one block (see EpmGalleryBlock) at a time - the EPM page's
 * hero, stats, calendar and carousel, and the gallery page's sections. Gated behind the ADMIN role
 * in SecurityConfig. The state -> district photo tree has its own controller
 * (AdminEpmGalleryRegionController).
 */
@RestController
@RequestMapping("/api/admin/epm/gallery")
@CrossOrigin(origins = "*")
public class AdminEpmGalleryController {

    private final EpmGalleryService epmGalleryService;
    private final EpmGalleryImportService epmGalleryImportService;

    @Autowired
    public AdminEpmGalleryController(EpmGalleryService epmGalleryService, EpmGalleryImportService epmGalleryImportService) {
        this.epmGalleryService = epmGalleryService;
        this.epmGalleryImportService = epmGalleryImportService;
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<EpmGalleryBlockDto>> listBlocks() {
        return ResponseEntity.ok(epmGalleryService.listBlocks());
    }

    /** Adds rows for any image files copied straight into the storage folders. */
    @PostMapping("/import")
    public ResponseEntity<EpmGalleryImportService.ImportResult> importFromStorage() {
        return ResponseEntity.ok(epmGalleryImportService.importFromStorage());
    }

    @GetMapping("/{block}")
    public ResponseEntity<List<EpmGalleryImageDto>> list(@PathVariable String block) {
        return ResponseEntity.ok(epmGalleryService.listByBlock(block));
    }

    @PostMapping(value = "/{block}", consumes = "multipart/form-data")
    public ResponseEntity<EpmGalleryImageDto> upload(
            @PathVariable String block,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(required = false) Integer displayOrder) {
        EpmGalleryImageDto created = epmGalleryService.upload(block, file, caption, city, state, featured, displayOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Body: the image ids of this block in their new display order. */
    @PutMapping(value = "/{block}/order", consumes = "application/json")
    public ResponseEntity<List<EpmGalleryImageDto>> reorder(@PathVariable String block, @RequestBody List<Long> orderedIds) {
        return ResponseEntity.ok(epmGalleryService.reorder(block, orderedIds));
    }

    @PutMapping(value = "/{block}/{id}", consumes = "application/json")
    public ResponseEntity<EpmGalleryImageDto> update(@PathVariable String block, @PathVariable Long id,
                                                     @Valid @RequestBody EpmGalleryImageUpdateDto dto) {
        return ResponseEntity.ok(epmGalleryService.updateMetadata(block, id, dto));
    }

    @PutMapping(value = "/{block}/{id}/file", consumes = "multipart/form-data")
    public ResponseEntity<EpmGalleryImageDto> replaceFile(@PathVariable String block, @PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(epmGalleryService.replaceFile(block, id, file));
    }

    @DeleteMapping("/{block}/{id}")
    public ResponseEntity<Void> delete(@PathVariable String block, @PathVariable Long id) {
        epmGalleryService.delete(block, id);
        return ResponseEntity.noContent().build();
    }
}
