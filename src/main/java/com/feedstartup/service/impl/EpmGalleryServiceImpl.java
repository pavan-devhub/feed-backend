package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmGalleryImage;
import com.feedstartup.repository.EpmGalleryImageRepository;
import com.feedstartup.service.EpmGalleryService;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final EpmGalleryImageRepository epmGalleryImageRepository;

    @Value("${epm.storage.gallery-dir}")
    private String galleryDir;

    @Autowired
    public EpmGalleryServiceImpl(EpmGalleryImageRepository epmGalleryImageRepository) {
        this.epmGalleryImageRepository = epmGalleryImageRepository;
    }

    @Override
    public List<EpmGalleryImageDto> list() {
        return epmGalleryImageRepository.findAllByOrderByDisplayOrderAscIdDesc().stream()
                .map(EpmGalleryImageDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public EpmGalleryImageDto upload(MultipartFile file, String caption, String city, String state,
                                      boolean featured, Integer displayOrder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase());
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP or GIF images are allowed");
        }

        Path dir = Paths.get(galleryDir);
        Path target = dir.resolve(UUID.randomUUID() + extension);

        try {
            Files.createDirectories(dir);
            file.transferTo(target);

            EpmGalleryImage image = new EpmGalleryImage();
            image.setImagePath(target.toString());
            image.setCaption(caption);
            image.setCity(city);
            image.setState(state);
            image.setFeatured(featured);
            image.setDisplayOrder(displayOrder != null ? displayOrder : 0);

            EpmGalleryImage saved = epmGalleryImageRepository.save(image);
            return EpmGalleryImageDto.from(saved);
        } catch (IOException e) {
            deleteQuietly(target);
            throw new RuntimeException("Failed to store the gallery image: " + e.getMessage(), e);
        }
    }

    @Override
    public EpmGalleryImageDto updateMetadata(Long id, String caption, String city, String state,
                                              Boolean featured, Integer displayOrder) {
        EpmGalleryImage image = findOrThrow(id);
        if (caption != null) image.setCaption(caption);
        if (city != null) image.setCity(city);
        if (state != null) image.setState(state);
        if (featured != null) image.setFeatured(featured);
        if (displayOrder != null) image.setDisplayOrder(displayOrder);
        return EpmGalleryImageDto.from(epmGalleryImageRepository.save(image));
    }

    @Override
    public StoredFile loadImageFile(Long id) {
        EpmGalleryImage image = findOrThrow(id);
        Path path = Paths.get(image.getImagePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Image file is missing on the server for gallery image " + id);
        }
        Resource resource = new FileSystemResource(path);
        String contentType = contentTypeFor(path.toString());
        return new StoredFile(resource, contentType, "epm-gallery-" + id + extensionOf(path.toString()));
    }

    @Override
    public void delete(Long id) {
        EpmGalleryImage image = findOrThrow(id);
        deleteQuietly(Paths.get(image.getImagePath()));
        epmGalleryImageRepository.delete(image);
    }

    private EpmGalleryImage findOrThrow(Long id) {
        return epmGalleryImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found: " + id));
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
}
