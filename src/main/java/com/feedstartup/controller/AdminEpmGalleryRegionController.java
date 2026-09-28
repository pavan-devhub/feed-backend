package com.feedstartup.controller;

import com.feedstartup.dto.EpmGalleryDistrictAdminDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryImageUpdateDto;
import com.feedstartup.dto.EpmGalleryRegionRequestDto;
import com.feedstartup.dto.EpmGalleryStateAdminDto;
import com.feedstartup.service.EpmGalleryRegionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The gallery page's state cards, their district cards, and each district's photos. Everything is
 * addressed by numeric id (the public routes use URL slugs instead). Gated behind the ADMIN role in
 * SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/epm/gallery-regions")
@CrossOrigin(origins = "*")
public class AdminEpmGalleryRegionController {

    private final EpmGalleryRegionService regionService;

    @Autowired
    public AdminEpmGalleryRegionController(EpmGalleryRegionService regionService) {
        this.regionService = regionService;
    }

    // --- states ---

    @GetMapping("/states")
    public ResponseEntity<List<EpmGalleryStateAdminDto>> listStates() {
        return ResponseEntity.ok(regionService.adminListStates());
    }

    @PostMapping(value = "/states", consumes = "application/json")
    public ResponseEntity<EpmGalleryStateAdminDto> createState(@Valid @RequestBody EpmGalleryRegionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regionService.createState(dto));
    }

    @PutMapping(value = "/states/{stateId}", consumes = "application/json")
    public ResponseEntity<EpmGalleryStateAdminDto> updateState(@PathVariable Long stateId,
                                                               @Valid @RequestBody EpmGalleryRegionRequestDto dto) {
        return ResponseEntity.ok(regionService.updateState(stateId, dto));
    }

    @DeleteMapping("/states/{stateId}")
    public ResponseEntity<Void> deleteState(@PathVariable Long stateId) {
        regionService.deleteState(stateId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/states/{stateId}/cover", consumes = "multipart/form-data")
    public ResponseEntity<EpmGalleryStateAdminDto> setCover(@PathVariable Long stateId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(regionService.setStateCover(stateId, file));
    }

    @DeleteMapping("/states/{stateId}/cover")
    public ResponseEntity<EpmGalleryStateAdminDto> removeCover(@PathVariable Long stateId) {
        return ResponseEntity.ok(regionService.removeStateCover(stateId));
    }

    // --- districts ---

    @PostMapping(value = "/states/{stateId}/districts", consumes = "application/json")
    public ResponseEntity<EpmGalleryDistrictAdminDto> createDistrict(@PathVariable Long stateId,
                                                                     @Valid @RequestBody EpmGalleryRegionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regionService.createDistrict(stateId, dto));
    }

    @PutMapping(value = "/districts/{districtId}", consumes = "application/json")
    public ResponseEntity<EpmGalleryDistrictAdminDto> updateDistrict(@PathVariable Long districtId,
                                                                     @Valid @RequestBody EpmGalleryRegionRequestDto dto) {
        return ResponseEntity.ok(regionService.updateDistrict(districtId, dto));
    }

    @DeleteMapping("/districts/{districtId}")
    public ResponseEntity<Void> deleteDistrict(@PathVariable Long districtId) {
        regionService.deleteDistrict(districtId);
        return ResponseEntity.noContent().build();
    }

    // --- district photos ---

    @GetMapping("/districts/{districtId}/photos")
    public ResponseEntity<List<EpmGalleryImageDto>> listPhotos(@PathVariable Long districtId) {
        return ResponseEntity.ok(regionService.listDistrictPhotos(districtId));
    }

    @PostMapping(value = "/districts/{districtId}/photos", consumes = "multipart/form-data")
    public ResponseEntity<EpmGalleryImageDto> uploadPhoto(@PathVariable Long districtId,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regionService.uploadDistrictPhoto(districtId, file, caption));
    }

    /** Body: every photo id of this district in its new display order. */
    @PutMapping(value = "/districts/{districtId}/photos/order", consumes = "application/json")
    public ResponseEntity<List<EpmGalleryImageDto>> reorderPhotos(@PathVariable Long districtId,
                                                                  @RequestBody List<Long> orderedIds) {
        return ResponseEntity.ok(regionService.reorderDistrictPhotos(districtId, orderedIds));
    }

    @PutMapping(value = "/districts/{districtId}/photos/{photoId}", consumes = "application/json")
    public ResponseEntity<EpmGalleryImageDto> updatePhoto(@PathVariable Long districtId, @PathVariable Long photoId,
                                                          @Valid @RequestBody EpmGalleryImageUpdateDto dto) {
        return ResponseEntity.ok(regionService.updateDistrictPhoto(districtId, photoId, dto));
    }

    @PutMapping(value = "/districts/{districtId}/photos/{photoId}/file", consumes = "multipart/form-data")
    public ResponseEntity<EpmGalleryImageDto> replacePhoto(@PathVariable Long districtId, @PathVariable Long photoId,
                                                           @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(regionService.replaceDistrictPhoto(districtId, photoId, file));
    }

    @DeleteMapping("/districts/{districtId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long districtId, @PathVariable Long photoId) {
        regionService.deleteDistrictPhoto(districtId, photoId);
        return ResponseEntity.noContent().build();
    }
}
