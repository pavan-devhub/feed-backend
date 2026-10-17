package com.feedstartup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpmGalleryStorageTest {

    // Stands in for storage/epm - gallery/ and carousel/ sit side by side inside it.
    @TempDir
    Path epmDir;

    private Path galleryDir;
    private Path carouselDir;
    private EpmGalleryStorage storage;

    @BeforeEach
    void setUp() {
        galleryDir = epmDir.resolve("gallery");
        carouselDir = epmDir.resolve("carousel");
        storage = new EpmGalleryStorage();
        ReflectionTestUtils.setField(storage, "galleryDir", galleryDir.toString());
        ReflectionTestUtils.setField(storage, "carouselDir", carouselDir.toString());
    }

    @Test
    void storesCarouselUploadsInTheirOwnFolderBesideTheGallery() throws IOException {
        MockMultipartFile upload = new MockMultipartFile("file", "slide.png", "image/png", png(30, 20));

        Path dir = storage.blockDir("epm-carousel");
        EpmGalleryStorage.StoredImage stored = storage.store(upload, dir, null);

        assertEquals(carouselDir.toAbsolutePath().normalize(), dir);
        assertTrue(Files.isRegularFile(carouselDir.resolve(stored.fileName())));
        assertFalse(Files.exists(galleryDir.resolve("epm-carousel")));
        // Every other block still lives inside the gallery folder.
        assertEquals(galleryDir.toAbsolutePath().normalize().resolve("epm-stats"), storage.blockDir("epm-stats"));
    }

    @Test
    void movesCarouselPhotosOutOfTheOldGalleryFolder() throws IOException {
        Path legacy = Files.createDirectories(galleryDir.resolve("epm-carousel"));
        Files.write(legacy.resolve("a.png"), new byte[]{1});
        Files.write(legacy.resolve("b.png"), new byte[]{2});

        assertEquals(2, storage.moveLegacyCarouselFiles());

        assertArrayEquals(new byte[]{1}, Files.readAllBytes(carouselDir.resolve("a.png")));
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(carouselDir.resolve("b.png")));
        assertFalse(Files.exists(legacy));
        // Nothing left to move the second time round.
        assertEquals(0, storage.moveLegacyCarouselFiles());
    }

    @Test
    void neverOverwritesACarouselPhotoWhenMoving() throws IOException {
        Path legacy = Files.createDirectories(galleryDir.resolve("epm-carousel"));
        Files.write(legacy.resolve("a.png"), new byte[]{1});
        Files.createDirectories(carouselDir);
        Files.write(carouselDir.resolve("a.png"), new byte[]{9});

        assertEquals(0, storage.moveLegacyCarouselFiles());

        assertArrayEquals(new byte[]{9}, Files.readAllBytes(carouselDir.resolve("a.png")));
        assertTrue(Files.isRegularFile(legacy.resolve("a.png")));
    }

    @Test
    void storesAnUploadUnderAFreshNameWithTheExtensionOfItsRealType() throws IOException {
        // Claims to be a JPEG called holiday.jpg, but the bytes are a PNG.
        MockMultipartFile upload = new MockMultipartFile("file", "holiday.jpg", "image/jpeg", png(40, 25));

        EpmGalleryStorage.StoredImage stored = storage.store(upload, storage.blockDir("epm-stats"), null);

        assertTrue(stored.fileName().endsWith(".png"));
        assertEquals("image/png", stored.contentType());
        assertEquals(40, stored.width());
        assertEquals(25, stored.height());
        assertTrue(Files.isRegularFile(storage.blockDir("epm-stats").resolve(stored.fileName())));
    }

    @Test
    void refusesAFileThatIsNotAnImage() {
        MockMultipartFile upload = new MockMultipartFile("file", "photo.png", "image/png",
                "<script>alert(1)</script> not really an image".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storage.store(upload, storage.blockDir("epm-stats"), null));
        assertTrue(!Files.exists(storage.blockDir("epm-stats")) || isEmpty(storage.blockDir("epm-stats")));
    }

    @Test
    void neverResolvesAPathOutsideItsFolder() {
        Path block = storage.blockDir("epm-moments");
        assertThrows(IllegalArgumentException.class, () -> storage.child(block, "../epm-stats/x.png"));
        assertThrows(IllegalArgumentException.class, () -> storage.child(block, ".."));
        assertThrows(IllegalArgumentException.class, () -> storage.blockDir("../../etc"));
        assertEquals(block.resolve("a.png"), storage.child(block, "a.png"));
    }

    private static boolean isEmpty(Path dir) {
        try (var files = Files.list(dir)) {
            return files.findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
