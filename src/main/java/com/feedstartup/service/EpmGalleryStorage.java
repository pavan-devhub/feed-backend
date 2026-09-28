package com.feedstartup.service;

import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmGalleryBlock;
import com.feedstartup.model.EpmGalleryDistrict;
import com.feedstartup.model.EpmGalleryState;
import com.feedstartup.util.ImageFiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * The on-disk half of the EPM gallery: where each image's file lives and how an upload is written
 * there. Which files exist - and everything about them - is the database's job (EpmGalleryImage,
 * EpmGalleryState, EpmGalleryDistrict); this class only ever touches the file a row names.
 * <pre>
 *   &lt;gallery-dir&gt;/&lt;block&gt;/&lt;file&gt;                                   block images
 *   &lt;gallery-dir&gt;/epm-gallery/&lt;state folder&gt;/&lt;cover file&gt;           state card cover
 *   &lt;gallery-dir&gt;/epm-gallery/&lt;state folder&gt;/&lt;district folder&gt;/&lt;file&gt;  district photos
 * </pre>
 */
@Component
public class EpmGalleryStorage {

    private static final long MAX_IMAGE_BYTES = 15L * 1024 * 1024;

    @Value("${epm.storage.gallery-dir}")
    private String galleryDir;

    /** What was written for an upload - everything the caller needs to fill in the image's row. */
    public record StoredImage(String fileName, String contentType, long size, Integer width, Integer height) {}

    public Path root() {
        return Paths.get(galleryDir).toAbsolutePath().normalize();
    }

    public Path blockDir(String blockId) {
        return child(root(), blockId);
    }

    public Path statesRoot() {
        return blockDir(EpmGalleryBlock.EPM_GALLERY.getId());
    }

    public Path stateDir(EpmGalleryState state) {
        return child(statesRoot(), state.getFolder());
    }

    public Path districtDir(EpmGalleryState state, EpmGalleryDistrict district) {
        return child(stateDir(state), district.getFolder());
    }

    /**
     * Resolves {@code name} inside {@code dir}, refusing anything that would land outside it. Names
     * come from the database rather than requests, but this keeps a bad row from ever reaching
     * another folder.
     */
    public Path child(Path dir, String name) {
        Path resolved = dir.resolve(name).normalize();
        if (!resolved.startsWith(dir) || resolved.equals(dir)) {
            throw new IllegalArgumentException("Invalid gallery path: " + name);
        }
        return resolved;
    }

    /**
     * Writes an uploaded image into {@code dir} under a fresh random name. The type is taken from
     * the file's own bytes, never the client's filename or content type.
     */
    public StoredImage store(MultipartFile file, Path dir, String namePrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Images must be 15 MB or smaller");
        }
        ImageFiles.ImageType type;
        try (InputStream in = file.getInputStream()) {
            type = ImageFiles.sniff(in.readNBytes(32))
                    .orElseThrow(() -> new IllegalArgumentException("Only " + ImageFiles.ALLOWED_TYPES_LABEL + " images are allowed"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the uploaded image: " + e.getMessage(), e);
        }

        String fileName = (namePrefix == null ? "" : namePrefix) + UUID.randomUUID() + type.extension();
        Path target = child(dir, fileName);
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            deleteQuietly(target);
            throw new RuntimeException("Failed to store the image: " + e.getMessage(), e);
        }
        int[] size = ImageFiles.pixelSizeOf(target);
        return new StoredImage(fileName, type.contentType(), file.getSize(),
                size == null ? null : size[0], size == null ? null : size[1]);
    }

    /** The same facts as {@link #store}, for a file that is already on disk (see the import). */
    public StoredImage describe(Path file) {
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            size = 0;
        }
        int[] pixels = ImageFiles.pixelSizeOf(file);
        String name = file.getFileName().toString();
        return new StoredImage(name, ImageFiles.contentTypeFor(name), size,
                pixels == null ? null : pixels[0], pixels == null ? null : pixels[1]);
    }

    public StoredFile load(Path path, String contentType) {
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Image file is missing from storage: " + path.getFileName());
        }
        String type = contentType != null ? contentType : ImageFiles.contentTypeFor(path.getFileName().toString());
        return new StoredFile(new FileSystemResource(path), type, path.getFileName().toString());
    }

    public void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create gallery folder: " + e.getMessage(), e);
        }
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for -
            // nothing is ever served without a database row pointing at it.
        }
    }

    public void deleteDirectoryQuietly(Path dir) {
        try {
            FileSystemUtils.deleteRecursively(dir);
        } catch (IOException ignored) {
            // Same as deleteQuietly - an orphaned folder is harmless.
        }
    }
}
