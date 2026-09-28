package com.feedstartup.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Small, dependency-free helpers shared by everything that stores or serves EPM gallery images:
 * recognising an image from its bytes, reading its pixel size, and turning folder/display names
 * into URL slugs.
 */
public final class ImageFiles {

    public static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp",
            ".gif", "image/gif",
            ".avif", "image/avif"
    );

    public static final String ALLOWED_TYPES_LABEL = "JPEG, PNG, WEBP, GIF or AVIF";

    private ImageFiles() {}

    /** One recognised image format: the extension a stored copy gets and the type it is served as. */
    public record ImageType(String extension, String contentType) {}

    /**
     * Identifies an image from its leading bytes rather than the client-supplied content type or
     * filename, so a renamed non-image can't be stored as one. Empty when it isn't a supported image.
     */
    public static Optional<ImageType> sniff(byte[] head) {
        if (head == null || head.length < 12) return Optional.empty();
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return Optional.of(new ImageType(".jpg", "image/jpeg"));
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return Optional.of(new ImageType(".png", "image/png"));
        }
        if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') {
            return Optional.of(new ImageType(".gif", "image/gif"));
        }
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return Optional.of(new ImageType(".webp", "image/webp"));
        }
        // ISO-BMFF: bytes 4-7 are "ftyp", then the major brand ("avif" for stills, "avis" for sequences).
        if (head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p'
                && head[8] == 'a' && head[9] == 'v' && head[10] == 'i' && (head[11] == 'f' || head[11] == 's')) {
            return Optional.of(new ImageType(".avif", "image/avif"));
        }
        return Optional.empty();
    }

    public static boolean isImageFile(Path p) {
        return Files.isRegularFile(p) && CONTENT_TYPE_BY_EXTENSION.containsKey(extensionOf(p.getFileName().toString()));
    }

    public static String contentTypeFor(String filename) {
        return CONTENT_TYPE_BY_EXTENSION.getOrDefault(extensionOf(filename), "image/jpeg");
    }

    /** Lower-cased extension including the dot, or "" when there is none. */
    public static String extensionOf(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    public static String stemOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    // --- pixel size ------------------------------------------------------------------------

    /** {width, height}, or null when the file's size can't be read - callers then assume a default. */
    public static int[] pixelSizeOf(Path photo) {
        try {
            return extensionOf(photo.getFileName().toString()).equals(".avif") ? avifSizeOf(photo) : imageIoSizeOf(photo);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** ImageIO has no AVIF reader, so read the size from the first "ispe" (image spatial extents)
     * box, which sits in the file's leading metadata: 4 bytes version/flags, then width and height
     * as big-endian ints. */
    private static int[] avifSizeOf(Path photo) throws IOException {
        byte[] head = new byte[16 * 1024];
        int read;
        try (InputStream in = Files.newInputStream(photo)) {
            read = in.readNBytes(head, 0, head.length);
        }
        for (int i = 0; i + 16 <= read; i++) {
            if (head[i] == 'i' && head[i + 1] == 's' && head[i + 2] == 'p' && head[i + 3] == 'e') {
                int width = ByteBuffer.wrap(head, i + 8, 4).getInt();
                int height = ByteBuffer.wrap(head, i + 12, 4).getInt();
                return width > 0 && height > 0 ? new int[]{width, height} : null;
            }
        }
        return null;
    }

    private static int[] imageIoSizeOf(Path photo) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(photo.toFile())) {
            Iterator<ImageReader> readers = in == null ? Collections.emptyIterator() : ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    // --- naming ----------------------------------------------------------------------------

    /** "East Godavari" / "east_godavari" / "east-godavari" all become {@code east-godavari}. */
    public static String slugOf(String name) {
        return name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    /** A folder someone typed with capitals or spaces is shown as typed; a slug is title-cased. */
    public static String displayNameOf(String folderName) {
        String name = folderName.trim();
        if (name.chars().anyMatch(c -> Character.isUpperCase(c) || c == ' ')) {
            return name;
        }
        return Arrays.stream(name.split("[-_]+"))
                .filter(w -> !w.isBlank())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }
}
