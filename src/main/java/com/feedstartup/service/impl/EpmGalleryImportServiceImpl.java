package com.feedstartup.service.impl;

import com.feedstartup.model.EpmGalleryBlock;
import com.feedstartup.model.EpmGalleryDistrict;
import com.feedstartup.model.EpmGalleryImage;
import com.feedstartup.model.EpmGalleryState;
import com.feedstartup.repository.EpmGalleryDistrictRepository;
import com.feedstartup.repository.EpmGalleryImageRepository;
import com.feedstartup.repository.EpmGalleryStateRepository;
import com.feedstartup.service.EpmGalleryImportService;
import com.feedstartup.service.EpmGalleryStorage;
import com.feedstartup.util.ImageFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Walks the gallery folder and adds a row for every image file that doesn't have one:
 * <ul>
 *   <li>{@code <block>/<file>} becomes a block image. If the old {@code <file-stem>.json} metadata
 *       sidecar sits next to it, its caption/city/state/featured/displayOrder/createdAt are copied
 *       into the row and the sidecar is deleted - the database is the only metadata store now.</li>
 *   <li>{@code epm-gallery/<state>/} becomes a state, {@code cover.*} inside it its cover, and each
 *       {@code epm-gallery/<state>/<district>/} a district whose photos keep their filename order.</li>
 * </ul>
 * Files that already have a row are left alone, so running it again only picks up new files.
 */
@Service
public class EpmGalleryImportServiceImpl implements EpmGalleryImportService {

    private static final Logger log = LoggerFactory.getLogger(EpmGalleryImportServiceImpl.class);
    private static final String LEGACY_COVER_STEM = "cover";

    private final EpmGalleryImageRepository imageRepository;
    private final EpmGalleryStateRepository stateRepository;
    private final EpmGalleryDistrictRepository districtRepository;
    private final EpmGalleryStorage storage;
    private final ObjectMapper objectMapper;

    @Autowired
    public EpmGalleryImportServiceImpl(EpmGalleryImageRepository imageRepository,
                                       EpmGalleryStateRepository stateRepository,
                                       EpmGalleryDistrictRepository districtRepository,
                                       EpmGalleryStorage storage,
                                       ObjectMapper objectMapper) {
        this.imageRepository = imageRepository;
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    // Deliberately not one big transaction: each row is committed by its own save before the
    // sidecar it came from is deleted, so a failure part-way through can never leave a deleted
    // sidecar whose metadata was rolled back.
    @Override
    public ImportResult importFromStorage() {
        Counter counter = new Counter();
        for (EpmGalleryBlock block : EpmGalleryBlock.values()) {
            importBlock(block, counter);
        }
        importRegions(counter);
        ImportResult result = new ImportResult(counter.images, counter.states, counter.districts, counter.sidecars);
        if (counter.images + counter.states + counter.districts + counter.sidecars > 0) {
            log.info("EPM gallery import: {} image(s), {} state(s), {} district(s) added; {} JSON sidecar(s) removed.",
                    result.imagesImported(), result.statesCreated(), result.districtsCreated(), result.sidecarsRemoved());
        }
        return result;
    }

    // --- block images ----------------------------------------------------------------------

    private void importBlock(EpmGalleryBlock block, Counter counter) {
        Path dir = storage.blockDir(block.getId());
        for (Path file : imagesIn(dir)) {
            String fileName = file.getFileName().toString();
            Path sidecar = dir.resolve(ImageFiles.stemOf(fileName) + ".json");
            if (!imageRepository.existsByBlockAndDistrictIdIsNullAndFileName(block.getId(), fileName)) {
                EpmGalleryImage image = new EpmGalleryImage();
                image.setBlock(block.getId());
                EpmGalleryServiceImpl.applyStored(image, storage.describe(file));
                image.setCreatedAt(lastModified(file));
                applySidecar(image, sidecar);
                imageRepository.save(image);
                counter.images++;
            }
            if (deleteSidecar(sidecar)) {
                counter.sidecars++;
            }
        }
        // A sidecar whose image is already gone has nothing left to describe.
        for (Path orphan : list(dir, p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))) {
            String stem = ImageFiles.stemOf(orphan.getFileName().toString());
            boolean hasImage = ImageFiles.CONTENT_TYPE_BY_EXTENSION.keySet().stream()
                    .anyMatch(ext -> Files.exists(dir.resolve(stem + ext)));
            if (!hasImage && deleteSidecar(orphan)) {
                counter.sidecars++;
            }
        }
    }

    private void applySidecar(EpmGalleryImage image, Path sidecar) {
        if (!Files.isRegularFile(sidecar)) return;
        try {
            JsonNode meta = objectMapper.readTree(sidecar.toFile());
            image.setCaption(text(meta, "caption"));
            image.setCity(text(meta, "city"));
            image.setState(text(meta, "state"));
            if (meta.hasNonNull("featured")) image.setFeatured(meta.get("featured").asBoolean());
            if (meta.hasNonNull("displayOrder")) image.setDisplayOrder(meta.get("displayOrder").asInt());
            String createdAt = text(meta, "createdAt");
            if (createdAt != null) {
                image.setCreatedAt(LocalDateTime.parse(createdAt));
            }
        } catch (RuntimeException e) {
            // A corrupt sidecar only loses its metadata - the image itself is still imported.
            log.warn("Could not read gallery metadata {}: {}", sidecar, e.getMessage());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asString().isBlank() ? null : value.asString();
    }

    private boolean deleteSidecar(Path sidecar) {
        try {
            return Files.deleteIfExists(sidecar);
        } catch (IOException e) {
            log.warn("Could not delete gallery metadata sidecar {}: {}", sidecar, e.getMessage());
            return false;
        }
    }

    // --- state -> district -> photo tree ---------------------------------------------------

    private void importRegions(Counter counter) {
        Path root = storage.statesRoot();
        int nextStateOrder = stateRepository.maxDisplayOrder() + 1;
        for (Path stateDir : childDirs(root)) {
            String folder = stateDir.getFileName().toString();
            EpmGalleryState state = stateRepository.findByFolder(folder).orElse(null);
            if (state == null) {
                String slug = ImageFiles.slugOf(folder);
                if (slug.isEmpty() || stateRepository.existsBySlug(slug)) {
                    log.warn("Skipping gallery state folder {} - its name is empty or clashes with another state", stateDir);
                    continue;
                }
                state = new EpmGalleryState();
                state.setFolder(folder);
                state.setSlug(slug);
                state.setName(ImageFiles.displayNameOf(folder));
                state.setDisplayOrder(nextStateOrder++);
                state = stateRepository.save(state);
                counter.states++;
            }
            if (state.getCoverFile() == null) {
                EpmGalleryState target = state;
                imagesIn(stateDir).stream()
                        .filter(p -> ImageFiles.stemOf(p.getFileName().toString()).equalsIgnoreCase(LEGACY_COVER_STEM))
                        .findFirst()
                        .ifPresent(cover -> {
                            target.setCoverFile(cover.getFileName().toString());
                            stateRepository.save(target);
                        });
            }
            importDistricts(state, stateDir, counter);
        }
    }

    private void importDistricts(EpmGalleryState state, Path stateDir, Counter counter) {
        int nextDistrictOrder = districtRepository.maxDisplayOrderInState(state.getId()) + 1;
        for (Path districtDir : childDirs(stateDir)) {
            String folder = districtDir.getFileName().toString();
            EpmGalleryDistrict district = districtRepository.findByStateIdAndFolder(state.getId(), folder).orElse(null);
            if (district == null) {
                String slug = ImageFiles.slugOf(folder);
                if (slug.isEmpty() || districtRepository.existsByStateIdAndSlug(state.getId(), slug)) {
                    log.warn("Skipping gallery district folder {} - its name is empty or clashes with another district", districtDir);
                    continue;
                }
                district = new EpmGalleryDistrict();
                district.setStateId(state.getId());
                district.setFolder(folder);
                district.setSlug(slug);
                district.setName(ImageFiles.displayNameOf(folder));
                district.setDisplayOrder(nextDistrictOrder++);
                district = districtRepository.save(district);
                counter.districts++;
            }
            int nextPhotoOrder = imageRepository.maxDisplayOrderInDistrict(district.getId()) + 1;
            for (Path file : imagesIn(districtDir)) {
                String fileName = file.getFileName().toString();
                if (imageRepository.existsByDistrictIdAndFileName(district.getId(), fileName)) continue;
                EpmGalleryImage photo = new EpmGalleryImage();
                photo.setBlock(EpmGalleryBlock.EPM_GALLERY.getId());
                photo.setDistrictId(district.getId());
                EpmGalleryServiceImpl.applyStored(photo, storage.describe(file));
                photo.setCreatedAt(lastModified(file));
                photo.setDisplayOrder(nextPhotoOrder++);
                imageRepository.save(photo);
                counter.images++;
            }
        }
    }

    // --- folder scanning -------------------------------------------------------------------

    /** Image files directly inside {@code dir}, in filename order. */
    private static List<Path> imagesIn(Path dir) {
        return list(dir, ImageFiles::isImageFile);
    }

    private static List<Path> childDirs(Path dir) {
        return list(dir, p -> Files.isDirectory(p) && !p.getFileName().toString().startsWith("."));
    }

    private static List<Path> list(Path dir, Predicate<Path> filter) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.filter(filter)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list gallery folder: " + e.getMessage(), e);
        }
    }

    private static LocalDateTime lastModified(Path file) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            return LocalDateTime.now();
        }
    }

    private static final class Counter {
        int images;
        int states;
        int districts;
        int sidecars;
    }
}
