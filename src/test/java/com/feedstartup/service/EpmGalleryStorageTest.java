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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpmGalleryStorageTest {

    @TempDir
    Path galleryDir;

    private EpmGalleryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new EpmGalleryStorage();
        ReflectionTestUtils.setField(storage, "galleryDir", galleryDir.toString());
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
