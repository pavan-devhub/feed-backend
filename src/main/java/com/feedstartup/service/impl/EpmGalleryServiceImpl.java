package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmGalleryBlock;
import com.feedstartup.model.EpmGalleryImage;
import com.feedstartup.service.EpmGalleryService;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * No database is involved anywhere in here - the gallery IS the folder tree under
 * {@code epm.storage.gallery-dir}: one subfolder per {@link EpmGalleryBlock} (e.g.
 * {@code epm-moments/}), each holding its own {@code <uuid>.<ext>} photos with an optional
 * {@code <uuid>.json} sidecar for caption/city/state/featured/displayOrder. A photo dropped
 * straight into a block's folder - no upload API call, no sidecar needed - still shows up, using
 * filename/file-time-derived defaults (see readMeta). Listing, looking up, and deleting a photo
 * all work by scanning that block's directory - there is nothing else to keep in sync.
 */
@Service
public class EpmGalleryServiceImpl implements EpmGalleryService {

    // Deliberately not trusting the client's original filename for the extension (or for
    // anything else) - the stored name is always a fresh UUID, and the extension is derived from
    // the sniffed content type instead, same defensive approach as PublicationServiceImpl.
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final ObjectMapper objectMapper;

    @Value("${epm.storage.gallery-dir}")
    private String galleryDir;

    @Autowired
    public EpmGalleryServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EpmGalleryImageDto> list() {
        List<Entry> all = new ArrayList<>();
        for (EpmGalleryBlock block : EpmGalleryBlock.values()) {
            all.addAll(loadAll(block.getId()));
        }
        return sorted(all).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<EpmGalleryImageDto> listByBlock(String block) {
        String blockId = requireBlock(block).getId();
        return sorted(loadAll(blockId)).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public EpmGalleryImageDto upload(String block, MultipartFile file, String caption, String city, String state,
                                      boolean featured, Integer displayOrder) {
        String blockId = requireBlock(block).getId();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase());
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP or GIF images are allowed");
        }

        Path dir = blockDir(blockId);
        String id = UUID.randomUUID().toString();
        Path target = dir.resolve(id + extension);

        EpmGalleryImage meta = new EpmGalleryImage();
        meta.setCaption(caption);
        meta.setCity(city);
        meta.setState(state);
        meta.setFeatured(featured);
        meta.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        meta.setCreatedAt(LocalDateTime.now());

        try {
            Files.createDirectories(dir);
            file.transferTo(target);
            writeMeta(dir, id, meta);
            return EpmGalleryImageDto.from(blockId, id, meta);
        } catch (IOException e) {
            deleteQuietly(target);
            deleteQuietly(dir.resolve(id + ".json"));
            throw new RuntimeException("Failed to store the gallery image: " + e.getMessage(), e);
        }
    }

    @Override
    public EpmGalleryImageDto updateMetadata(String block, String id, String caption, String city, String state,
                                              Boolean featured, Integer displayOrder) {
        String blockId = requireBlock(block).getId();
        Path dir = blockDir(blockId);
        Path imagePath = findImageFile(dir, id);
        EpmGalleryImage meta = readMeta(dir, id, imagePath);
        if (caption != null) meta.setCaption(caption);
        if (city != null) meta.setCity(city);
        if (state != null) meta.setState(state);
        if (featured != null) meta.setFeatured(featured);
        if (displayOrder != null) meta.setDisplayOrder(displayOrder);
        writeMeta(dir, id, meta);
        return EpmGalleryImageDto.from(blockId, id, meta);
    }

    @Override
    public StoredFile loadImageFile(String block, String id) {
        String blockId = requireBlock(block).getId();
        Path dir = blockDir(blockId);
        Path path = findImageFile(dir, id);
        Resource resource = new FileSystemResource(path);
        String contentType = contentTypeFor(path.toString());
        return new StoredFile(resource, contentType, "epm-" + blockId + "-" + id + extensionOf(path.toString()));
    }

    @Override
    public void delete(String block, String id) {
        String blockId = requireBlock(block).getId();
        Path dir = blockDir(blockId);
        Path imagePath = findImageFile(dir, id);
        deleteQuietly(imagePath);
        deleteQuietly(dir.resolve(id + ".json"));
    }

    // --- folder scanning -------------------------------------------------------------------

    private static EpmGalleryBlock requireBlock(String block) {
        return EpmGalleryBlock.fromId(block)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unknown gallery block \"" + block + "\" - must be one of: " + EpmGalleryBlock.allowedIdsJoined()));
    }

    private Path blockDir(String blockId) {
        return Paths.get(galleryDir, blockId);
    }

    private List<Entry> loadAll(String blockId) {
        Path dir = blockDir(blockId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(dir)) {
            return paths
                    .filter(EpmGalleryServiceImpl::isImageFile)
                    .map(p -> new Entry(blockId, idOf(p), readMeta(dir, idOf(p), p)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list gallery images: " + e.getMessage(), e);
        }
    }

    private static List<Entry> sorted(List<Entry> entries) {
        return entries.stream()
                .sorted(Comparator
                        .comparingInt((Entry e) -> e.meta().getDisplayOrder())
                        .thenComparing((Entry e) -> e.meta().getCreatedAt(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private EpmGalleryImageDto toDto(Entry e) {
        return EpmGalleryImageDto.from(e.block(), e.id(), e.meta());
    }

    private Path findImageFile(Path dir, String id) {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> paths = Files.list(dir)) {
                return paths
                        .filter(p -> isImageFile(p) && idOf(p).equals(id))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found: " + id));
            } catch (IOException e) {
                throw new RuntimeException("Failed to look up gallery image: " + e.getMessage(), e);
            }
        }
        throw new ResourceNotFoundException("Gallery image not found: " + id);
    }

    private EpmGalleryImage readMeta(Path dir, String id, Path imagePath) {
        Path metaPath = dir.resolve(id + ".json");
        if (Files.exists(metaPath)) {
            try {
                return objectMapper.readValue(metaPath.toFile(), EpmGalleryImage.class);
            } catch (JacksonException ignored) {
                // A corrupt/unreadable sidecar shouldn't break the whole gallery - fall back to
                // defaults below instead of failing the request.
            }
        }
        EpmGalleryImage fallback = new EpmGalleryImage();
        try {
            fallback.setCreatedAt(LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(imagePath).toInstant(), ZoneId.systemDefault()));
        } catch (IOException ignored) {
            fallback.setCreatedAt(LocalDateTime.now());
        }
        return fallback;
    }

    private void writeMeta(Path dir, String id, EpmGalleryImage meta) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve(id + ".json").toFile(), meta);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to write gallery image metadata: " + e.getMessage(), e);
        }
    }

    private static boolean isImageFile(Path p) {
        if (!Files.isRegularFile(p)) return false;
        String name = p.getFileName().toString().toLowerCase();
        return EXTENSION_BY_CONTENT_TYPE.values().stream().anyMatch(name::endsWith);
    }

    private static String idOf(Path p) {
        String filename = p.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot) : "";
    }

    private String contentTypeFor(String path) {
        String ext = extensionOf(path).toLowerCase();
        return switch (ext) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            default -> "image/jpeg";
        };
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for.
        }
    }

    private record Entry(String block, String id, EpmGalleryImage meta) {}
}
