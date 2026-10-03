package com.feedstartup.controller;

import com.feedstartup.dto.EpmPageVideoDto;
import com.feedstartup.service.EpmPageVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Uploads/replaces/removes the video at the top of the EPM page. Gated behind the ADMIN role in
 * SecurityConfig; the public page reads it through EpmVideoController.
 */
@RestController
@RequestMapping("/api/admin/epm/video")
@CrossOrigin(origins = "*")
public class AdminEpmVideoController {

    private final EpmPageVideoService epmPageVideoService;

    @Autowired
    public AdminEpmVideoController(EpmPageVideoService epmPageVideoService) {
        this.epmPageVideoService = epmPageVideoService;
    }

    /** 204 No Content while the page still plays its built-in video. */
    @GetMapping
    public ResponseEntity<EpmPageVideoDto> get() {
        return epmPageVideoService.getHeroVideo()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping(consumes = "multipart/form-data")
    public ResponseEntity<EpmPageVideoDto> replace(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(epmPageVideoService.replaceHeroVideo(file));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        epmPageVideoService.deleteHeroVideo();
        return ResponseEntity.noContent().build();
    }
}
