package com.feedstartup.controller;

import com.feedstartup.dto.EpmPageVideoDto;
import com.feedstartup.service.EpmPageVideoService;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints behind the video at the top of the EPM page. The video is uploaded through the
 * admin panel (AdminEpmVideoController); until then there is nothing here and the page plays its
 * built-in one.
 */
@RestController
@RequestMapping("/api/epm/video")
@CrossOrigin(origins = "*")
public class EpmVideoController {

    private final EpmPageVideoService epmPageVideoService;

    @Autowired
    public EpmVideoController(EpmPageVideoService epmPageVideoService) {
        this.epmPageVideoService = epmPageVideoService;
    }

    /** 204 No Content while no video has been uploaded. */
    @GetMapping
    public ResponseEntity<EpmPageVideoDto> get() {
        return epmPageVideoService.getHeroVideo()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // Returning the file as a Resource lets Spring answer the browser's byte-range requests
    // (206 Partial Content), which a <video> needs to start playing before it has the whole file
    // and to seek - Safari won't play it at all without them.
    @GetMapping("/file")
    public ResponseEntity<Resource> stream() {
        StoredFile file = epmPageVideoService.loadHeroVideoFile();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }
}
