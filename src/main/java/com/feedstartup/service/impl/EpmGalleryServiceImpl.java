package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmGalleryBlockDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryImageUpdateDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmGalleryBlock;
import com.feedstartup.model.EpmGalleryImage;
import com.feedstartup.repository.EpmGalleryImageRepository;
import com.feedstartup.service.EpmGalleryService;
import com.feedstartup.service.EpmGalleryStorage;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Database-backed block images: each row in epm_gallery_images names a file under
 * {@code <gallery-dir>/<block>/}. District photos share the table (see EpmGalleryRegionServiceImpl)
 * but are always excluded here by their non-null districtId.
 */
@Service
public class EpmGalleryServiceImpl implements EpmGalleryService {

    // Lowest display order first; among equals, the newest upload first (the order the old
    // folder-based gallery used, so imported photos keep their places).
    private static final Comparator<EpmGalleryImage> DISPLAY_ORDER = Comparator
            .comparingInt(EpmGalleryImage::getDisplayOrder)
            .thenComparing(EpmGalleryImage::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(EpmGalleryImage::getId);

    private final EpmGalleryImageRepository imageRepository;
    private final EpmGalleryStorage storage;

    @Autowired
    public EpmGalleryServiceImpl(EpmGalleryImageRepository imageRepository, EpmGalleryStorage storage) {
        this.imageRepository = imageRepository;
        this.storage = storage;
    }

    @Override
    public List<EpmGalleryImageDto> list() {
        List<String> galleryBlocks = Arrays.stream(EpmGalleryBlock.values())
                .filter(b -> b.getPage() == EpmGalleryBlock.Page.GALLERY_PAGE)
                .map(EpmGalleryBlock::getId)
                .toList();
        return imageRepository.findByBlockInAndDistrictIdIsNull(galleryBlocks).stream()
                .sorted(DISPLAY_ORDER)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EpmGalleryImageDto> listByBlock(String block) {
        return sortedIn(requireBlock(block)).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<EpmGalleryBlockDto> listBlocks() {
        return Arrays.stream(EpmGalleryBlock.values())
                .map(b -> EpmGalleryBlockDto.from(b, imageRepository.countByBlockAndDistrictIdIsNull(b.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EpmGalleryImageDto upload(String block, MultipartFile file, String caption, String city, String state,
                                     boolean featured, Integer displayOrder) {
        EpmGalleryBlock target = requireBlock(block);
        Integer max = target.getMaxImages();
        if (max != null && imageRepository.countByBlockAndDistrictIdIsNull(target.getId()) >= max) {
            throw new IllegalArgumentException("\"" + target.getLabel() + "\" holds at most " + max
                    + (max == 1 ? " image" : " images") + " - replace or delete an existing one first");
        }

        Path dir = storage.blockDir(target.getId());
        EpmGalleryStorage.StoredImage stored = storage.store(file, dir, null);

        EpmGalleryImage image = new EpmGalleryImage();
        image.setBlock(target.getId());
        applyStored(image, stored);
        image.setCaption(blankToNull(caption));
        image.setCity(blankToNull(city));
        image.setState(blankToNull(state));
        image.setFeatured(featured);
        image.setDisplayOrder(displayOrder != null ? displayOrder : imageRepository.maxDisplayOrderInBlock(target.getId()) + 1);
        try {
            return toDto(imageRepository.save(image));
        } catch (RuntimeException e) {
            storage.deleteQuietly(storage.child(dir, stored.fileName()));
            throw e;
        }
    }

    @Override
    @Transactional
    public EpmGalleryImageDto updateMetadata(String block, Long id, EpmGalleryImageUpdateDto dto) {
        EpmGalleryImage image = findOrThrow(requireBlock(block), id);
        if (dto.caption() != null) image.setCaption(blankToNull(dto.caption()));
        if (dto.city() != null) image.setCity(blankToNull(dto.city()));
        if (dto.state() != null) image.setState(blankToNull(dto.state()));
        if (dto.featured() != null) image.setFeatured(dto.featured());
        if (dto.displayOrder() != null) image.setDisplayOrder(dto.displayOrder());
        return toDto(imageRepository.save(image));
    }

    @Override
    @Transactional
    public EpmGalleryImageDto replaceFile(String block, Long id, MultipartFile file) {
        EpmGalleryBlock target = requireBlock(block);
        EpmGalleryImage image = findOrThrow(target, id);
        Path dir = storage.blockDir(target.getId());
        String oldFile = image.getFileName();

        EpmGalleryStorage.StoredImage stored = storage.store(file, dir, null);
        applyStored(image, stored);
        EpmGalleryImage saved;
        try {
            saved = imageRepository.saveAndFlush(image);
        } catch (RuntimeException e) {
            storage.deleteQuietly(storage.child(dir, stored.fileName()));
            throw e;
        }
        storage.deleteQuietly(storage.child(dir, oldFile));
        return toDto(saved);
    }

    @Override
    @Transactional
    public List<EpmGalleryImageDto> reorder(String block, List<Long> orderedIds) {
        EpmGalleryBlock target = requireBlock(block);
        Map<Long, EpmGalleryImage> byId = imageRepository.findByBlockAndDistrictIdIsNull(target.getId()).stream()
                .collect(Collectors.toMap(EpmGalleryImage::getId, Function.identity()));
        if (orderedIds == null || orderedIds.size() != byId.size() || !byId.keySet().equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("The new order must list every image in \"" + target.getLabel() + "\" exactly once");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setDisplayOrder(i);
        }
        imageRepository.saveAll(byId.values());
        return listByBlock(target.getId());
    }

    @Override
    public StoredFile loadImageFile(String block, Long id) {
        EpmGalleryBlock target = requireBlock(block);
        EpmGalleryImage image = findOrThrow(target, id);
        return storage.load(storage.child(storage.blockDir(target.getId()), image.getFileName()), image.getContentType());
    }

    @Override
    @Transactional
    public void delete(String block, Long id) {
        EpmGalleryBlock target = requireBlock(block);
        EpmGalleryImage image = findOrThrow(target, id);
        imageRepository.delete(image);
        imageRepository.flush();
        storage.deleteQuietly(storage.child(storage.blockDir(target.getId()), image.getFileName()));
    }

    // --- helpers ---------------------------------------------------------------------------

    private List<EpmGalleryImage> sortedIn(EpmGalleryBlock block) {
        return imageRepository.findByBlockAndDistrictIdIsNull(block.getId()).stream()
                .sorted(DISPLAY_ORDER)
                .collect(Collectors.toList());
    }

    private EpmGalleryImage findOrThrow(EpmGalleryBlock block, Long id) {
        return imageRepository.findByIdAndBlockAndDistrictIdIsNull(id, block.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found: " + id));
    }

    private static EpmGalleryBlock requireBlock(String block) {
        return EpmGalleryBlock.fromId(block)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unknown gallery block \"" + block + "\" - must be one of: " + EpmGalleryBlock.allowedIdsJoined()));
    }

    private EpmGalleryImageDto toDto(EpmGalleryImage image) {
        return EpmGalleryImageDto.from(image, "/api/epm/gallery/" + image.getBlock() + "/" + image.getId() + "/file");
    }

    static void applyStored(EpmGalleryImage image, EpmGalleryStorage.StoredImage stored) {
        image.setFileName(stored.fileName());
        image.setContentType(stored.contentType());
        image.setFileSize(stored.size());
        image.setWidth(stored.width());
        image.setHeight(stored.height());
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
