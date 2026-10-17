package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmPageVideoDto;
import com.feedstartup.model.EpmPageVideo;
import com.feedstartup.repository.EpmPageVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Where the EPM page video's file lives: its own video/ folder inside the EPM folder. */
class EpmPageVideoServiceImplTest {

    // Stands in for storage/epm - the video folder is video/ inside it.
    @TempDir
    Path epmDir;

    private Path videoDir;
    private final EpmPageVideoRepository repository = mock(EpmPageVideoRepository.class);
    private final EpmPageVideoServiceImpl service = new EpmPageVideoServiceImpl(repository);

    @BeforeEach
    void setUp() {
        videoDir = epmDir.resolve("video");
        ReflectionTestUtils.setField(service, "videoDir", videoDir.toString());
    }

    @Test
    void storesAnUploadInTheVideoFolder() throws IOException {
        when(repository.findById(EpmPageVideo.HERO)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(EpmPageVideo.class))).thenAnswer(call -> call.getArgument(0));
        MockMultipartFile upload = new MockMultipartFile("file", "intro.mp4", "video/mp4", mp4());

        EpmPageVideoDto saved = service.replaceHeroVideo(upload);

        assertTrue(Files.isRegularFile(videoDir.resolve(saved.fileName())));
        assertFalse(Files.exists(epmDir.resolve(saved.fileName())));
    }

    @Test
    void movesAVideoStoredStraightInTheEpmFolderIntoTheVideoFolder() throws IOException {
        Files.write(epmDir.resolve("epm-hero-old.mp4"), mp4());
        when(repository.findById(EpmPageVideo.HERO)).thenReturn(Optional.of(video("epm-hero-old.mp4")));

        assertTrue(service.moveLegacyVideoFile());

        assertArrayEquals(mp4(), Files.readAllBytes(videoDir.resolve("epm-hero-old.mp4")));
        assertFalse(Files.exists(epmDir.resolve("epm-hero-old.mp4")));
        assertEquals("epm-hero-old.mp4", service.loadHeroVideoFile().filename());
        // Already moved - nothing to do the second time round.
        assertFalse(service.moveLegacyVideoFile());
    }

    @Test
    void movesNothingWhenNoVideoHasBeenUploaded() throws IOException {
        Files.write(epmDir.resolve("epm-hero-stray.mp4"), mp4());
        when(repository.findById(EpmPageVideo.HERO)).thenReturn(Optional.empty());

        assertFalse(service.moveLegacyVideoFile());

        assertTrue(Files.exists(epmDir.resolve("epm-hero-stray.mp4")));
        assertFalse(Files.exists(videoDir));
    }

    private static EpmPageVideo video(String fileName) {
        EpmPageVideo video = new EpmPageVideo(EpmPageVideo.HERO);
        video.setFileName(fileName);
        video.setContentType("video/mp4");
        return video;
    }

    /** Just enough of an MP4 header for VideoFiles to recognise it. */
    private static byte[] mp4() {
        byte[] head = "\0\0\0\u0018ftypisom".getBytes(StandardCharsets.ISO_8859_1);
        return Arrays.copyOf(head, 64);
    }
}
