package com.feedstartup.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageFilesTest {

    @TempDir
    Path dir;

    @Test
    void recognisesEachSupportedFormatFromItsBytes() throws IOException {
        assertEquals(".png", ImageFiles.sniff(png(2, 2)).orElseThrow().extension());
        assertEquals("image/jpeg", ImageFiles.sniff(pad(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})).orElseThrow().contentType());
        assertEquals(".gif", ImageFiles.sniff(pad("GIF89a".getBytes(StandardCharsets.US_ASCII))).orElseThrow().extension());
        assertEquals(".webp", ImageFiles.sniff(pad("RIFF\0\0\0\0WEBP".getBytes(StandardCharsets.US_ASCII))).orElseThrow().extension());
        assertEquals(".avif", ImageFiles.sniff(pad("\0\0\0 ftypavif".getBytes(StandardCharsets.US_ASCII))).orElseThrow().extension());
    }

    @Test
    void rejectsNonImagesWhateverTheyAreCalled() {
        assertTrue(ImageFiles.sniff(pad("<html><body>".getBytes(StandardCharsets.US_ASCII))).isEmpty());
        assertTrue(ImageFiles.sniff(new byte[]{1, 2, 3}).isEmpty());
        assertTrue(ImageFiles.sniff(null).isEmpty());
    }

    @Test
    void readsPixelSizeFromPngAndAvifAndGivesUpOnGarbage() throws IOException {
        Path pngFile = Files.write(dir.resolve("a.png"), png(30, 20));
        Path avifFile = Files.write(dir.resolve("b.avif"), avif(1376, 768));
        Path broken = Files.write(dir.resolve("c.webp"), new byte[]{1, 2, 3});

        assertArrayEquals(new int[]{30, 20}, ImageFiles.pixelSizeOf(pngFile));
        assertArrayEquals(new int[]{1376, 768}, ImageFiles.pixelSizeOf(avifFile));
        assertNull(ImageFiles.pixelSizeOf(broken));
    }

    @Test
    void turnsFolderNamesIntoSlugsAndReadableNames() {
        assertEquals("east-godavari", ImageFiles.slugOf("East Godavari"));
        assertEquals("east-godavari", ImageFiles.slugOf("east_godavari"));
        assertEquals("", ImageFiles.slugOf("  ..  "));
        assertEquals("West Godavari", ImageFiles.displayNameOf("west-godavari"));
        // a folder typed by hand keeps its own spelling
        assertEquals("East Godavari", ImageFiles.displayNameOf("East Godavari"));
    }

    @Test
    void mapsExtensionsToContentTypes() {
        assertEquals("image/avif", ImageFiles.contentTypeFor("01.AVIF"));
        assertEquals("image/jpeg", ImageFiles.contentTypeFor("photo.jpeg"));
        assertEquals("cover", ImageFiles.stemOf("cover.avif"));
    }

    // --- helpers ---------------------------------------------------------------------------

    private static byte[] pad(byte[] head) {
        return Arrays.copyOf(head, 32);
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    /** Just enough of an AVIF: a leading "ispe" box carrying the size. */
    private static byte[] avif(int width, int height) {
        ByteBuffer box = ByteBuffer.allocate(20);
        box.putInt(20).put("ispe".getBytes(StandardCharsets.US_ASCII)).putInt(0).putInt(width).putInt(height);
        return box.array();
    }
}
