package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmPageVideoDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmPageVideo;
import com.feedstartup.repository.EpmPageVideoRepository;
import com.feedstartup.service.EpmPageVideoService;
import com.feedstartup.service.StoredFile;
import com.feedstartup.util.VideoFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * Database-backed EPM page video: the epm_page_videos row names a file sitting directly in
 * {@code epm.storage.video-dir} (the EPM storage root, e.g. {@code storage/epm/epm-hero-<uuid>.mp4}).
 */
@Service
public class EpmPageVideoServiceImpl implements EpmPageVideoService {

    // Uploads are also capped by spring.servlet.multipart.max-file-size - keep the two in step.
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    private static final String HERO_FILE_URL = "/api/epm/video/file";

    private final EpmPageVideoRepository videoRepository;

    @Value("${epm.storage.video-dir}")
    private String videoDir;

    @Autowired
    public EpmPageVideoServiceImpl(EpmPageVideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Override
    public Optional<EpmPageVideoDto> getHeroVideo() {
        return videoRepository.findById(EpmPageVideo.HERO).map(v -> EpmPageVideoDto.from(v, HERO_FILE_URL));
    }

    @Override
    @Transactional
    public EpmPageVideoDto replaceHeroVideo(MultipartFile file) {
        VideoFiles.VideoType type = sniff(file);
        EpmPageVideo video = videoRepository.findById(EpmPageVideo.HERO).orElseGet(() -> new EpmPageVideo(EpmPageVideo.HERO));
        String oldFile = video.getFileName();

        Path target = inRoot(EpmPageVideo.HERO + "-" + UUID.randomUUID() + type.extension());
        try {
            Files.createDirectories(root());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            deleteQuietly(target);
            throw new RuntimeException("Failed to store the video: " + e.getMessage(), e);
        }

        video.setFileName(target.getFileName().toString());
        video.setContentType(type.contentType());
        video.setFileSize(file.getSize());
        EpmPageVideo saved;
        try {
            saved = videoRepository.saveAndFlush(video);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
        if (oldFile != null) {
            deleteQuietly(inRoot(oldFile));
        }
        return EpmPageVideoDto.from(saved, HERO_FILE_URL);
    }

    @Override
    public StoredFile loadHeroVideoFile() {
        EpmPageVideo video = findHeroOrThrow();
        Path path = inRoot(video.getFileName());
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Video file is missing from storage: " + video.getFileName());
        }
        String type = video.getContentType() != null ? video.getContentType() : "video/mp4";
        return new StoredFile(new FileSystemResource(path), type, video.getFileName());
    }

    @Override
    @Transactional
    public void deleteHeroVideo() {
        EpmPageVideo video = findHeroOrThrow();
        videoRepository.delete(video);
        videoRepository.flush();
        deleteQuietly(inRoot(video.getFileName()));
    }

    // --- helpers ---------------------------------------------------------------------------

    private EpmPageVideo findHeroOrThrow() {
        return videoRepository.findById(EpmPageVideo.HERO)
                .orElseThrow(() -> new ResourceNotFoundException("No EPM page video has been uploaded"));
    }

    /** The type is taken from the file's own bytes, never the client's filename or content type. */
    private static VideoFiles.VideoType sniff(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A video file is required");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("Videos must be 50 MB or smaller");
        }
        try (InputStream in = file.getInputStream()) {
            return VideoFiles.sniff(in.readNBytes(VideoFiles.HEAD_BYTES))
                    .orElseThrow(() -> new IllegalArgumentException("Only " + VideoFiles.ALLOWED_TYPES_LABEL + " videos are allowed"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the uploaded video: " + e.getMessage(), e);
        }
    }

    private Path root() {
        return Paths.get(videoDir).toAbsolutePath().normalize();
    }

    /** Resolves a file name inside the EPM storage root, refusing anything that would land outside it. */
    private Path inRoot(String fileName) {
        Path root = root();
        Path resolved = root.resolve(fileName).normalize();
        if (!root.equals(resolved.getParent())) {
            throw new IllegalArgumentException("Invalid video path: " + fileName);
        }
        return resolved;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup - nothing is ever served without a database row pointing at it.
        }
    }
}
