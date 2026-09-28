package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmGalleryDistrictAdminDto;
import com.feedstartup.dto.EpmGalleryDistrictDetailDto;
import com.feedstartup.dto.EpmGalleryDistrictDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryImageUpdateDto;
import com.feedstartup.dto.EpmGalleryPhotoDto;
import com.feedstartup.dto.EpmGalleryRegionRequestDto;
import com.feedstartup.dto.EpmGalleryStateAdminDto;
import com.feedstartup.dto.EpmGalleryStateDto;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmGalleryBlock;
import com.feedstartup.model.EpmGalleryDistrict;
import com.feedstartup.model.EpmGalleryImage;
import com.feedstartup.model.EpmGalleryState;
import com.feedstartup.repository.EpmGalleryDistrictRepository;
import com.feedstartup.repository.EpmGalleryImageRepository;
import com.feedstartup.repository.EpmGalleryStateRepository;
import com.feedstartup.service.EpmGalleryRegionService;
import com.feedstartup.service.EpmGalleryStorage;
import com.feedstartup.service.StoredFile;
import com.feedstartup.util.ImageFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.feedstartup.service.impl.EpmGalleryServiceImpl.applyStored;
import static com.feedstartup.service.impl.EpmGalleryServiceImpl.blankToNull;

/**
 * Database-backed state -> district -> photo tree. A state or district only appears on the public
 * page once it holds at least one photo; the admin views list everything.
 */
@Service
public class EpmGalleryRegionServiceImpl implements EpmGalleryRegionService {

    private static final String URL_PREFIX = "/api/epm/gallery/states/";
    private static final String COVER_PREFIX = "cover-";

    private static final Comparator<EpmGalleryImage> PHOTO_ORDER = Comparator
            .comparingInt(EpmGalleryImage::getDisplayOrder)
            .thenComparing(EpmGalleryImage::getId);

    private final EpmGalleryStateRepository stateRepository;
    private final EpmGalleryDistrictRepository districtRepository;
    private final EpmGalleryImageRepository imageRepository;
    private final EpmGalleryStorage storage;

    @Autowired
    public EpmGalleryRegionServiceImpl(EpmGalleryStateRepository stateRepository,
                                       EpmGalleryDistrictRepository districtRepository,
                                       EpmGalleryImageRepository imageRepository,
                                       EpmGalleryStorage storage) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.imageRepository = imageRepository;
        this.storage = storage;
    }

    // =========================================================================================
    // public
    // =========================================================================================

    @Override
    public List<EpmGalleryStateDto> listStates() {
        Tree tree = loadTree(stateRepository.findAllByOrderByDisplayOrderAscNameAsc());
        return tree.states().stream()
                .map(s -> toPublicState(s, tree))
                .filter(s -> s.photoCount() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public EpmGalleryStateDto getState(String stateSlug) {
        EpmGalleryState state = requireStateBySlug(stateSlug);
        return toPublicState(state, loadTree(List.of(state)));
    }

    @Override
    public EpmGalleryDistrictDetailDto getDistrict(String stateSlug, String districtSlug) {
        EpmGalleryState state = requireStateBySlug(stateSlug);
        EpmGalleryDistrict district = requireDistrictBySlug(state, districtSlug);
        List<EpmGalleryPhotoDto> photos = photosOf(district.getId()).stream()
                .map(p -> new EpmGalleryPhotoDto(p.getId(), photoUrl(state, district, p), p.getWidth(), p.getHeight(), p.getCaption()))
                .collect(Collectors.toList());
        return new EpmGalleryDistrictDetailDto(district.getSlug(), district.getName(), state.getSlug(), state.getName(),
                photos.size(), photos);
    }

    @Override
    public StoredFile loadStateCover(String stateSlug) {
        EpmGalleryState state = requireStateBySlug(stateSlug);
        if (state.getCoverFile() != null) {
            Path cover = storage.child(storage.stateDir(state), state.getCoverFile());
            if (Files.isRegularFile(cover)) {
                return storage.load(cover, null);
            }
        }
        for (EpmGalleryDistrict district : districtRepository.findByStateIdOrderByDisplayOrderAscNameAsc(state.getId())) {
            Optional<EpmGalleryImage> first = photosOf(district.getId()).stream().findFirst();
            if (first.isPresent()) {
                return loadPhotoFile(state, district, first.get());
            }
        }
        throw new ResourceNotFoundException("No cover image for state: " + stateSlug);
    }

    @Override
    public StoredFile loadPhoto(String stateSlug, String districtSlug, Long photoId) {
        EpmGalleryState state = requireStateBySlug(stateSlug);
        EpmGalleryDistrict district = requireDistrictBySlug(state, districtSlug);
        EpmGalleryImage photo = imageRepository.findByIdAndDistrictId(photoId, district.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Gallery photo not found: " + photoId));
        return loadPhotoFile(state, district, photo);
    }

    // =========================================================================================
    // admin: states
    // =========================================================================================

    @Override
    public List<EpmGalleryStateAdminDto> adminListStates() {
        Tree tree = loadTree(stateRepository.findAllByOrderByDisplayOrderAscNameAsc());
        return tree.states().stream().map(s -> toAdminState(s, tree)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EpmGalleryStateAdminDto createState(EpmGalleryRegionRequestDto dto) {
        String name = dto.name().trim();
        String slug = requireSlug(name);
        if (stateRepository.existsBySlug(slug)) {
            throw new ConflictException("A state called \"" + name + "\" already exists");
        }
        EpmGalleryState state = new EpmGalleryState();
        state.setName(name);
        state.setSlug(slug);
        state.setFolder(uniqueFolder(slug, f -> stateRepository.existsByFolder(f) || Files.exists(storage.statesRoot().resolve(f))));
        state.setDisplayOrder(dto.displayOrder() != null ? dto.displayOrder() : stateRepository.maxDisplayOrder() + 1);
        EpmGalleryState saved = stateRepository.save(state);
        storage.createDirectories(storage.stateDir(saved));
        return toAdminState(saved, loadTree(List.of(saved)));
    }

    @Override
    @Transactional
    public EpmGalleryStateAdminDto updateState(Long stateId, EpmGalleryRegionRequestDto dto) {
        EpmGalleryState state = requireState(stateId);
        String name = dto.name().trim();
        String slug = requireSlug(name);
        if (!slug.equals(state.getSlug()) && stateRepository.existsBySlug(slug)) {
            throw new ConflictException("A state called \"" + name + "\" already exists");
        }
        // Only the name and URL slug change - the folder on disk stays where it is.
        state.setName(name);
        state.setSlug(slug);
        if (dto.displayOrder() != null) state.setDisplayOrder(dto.displayOrder());
        EpmGalleryState saved = stateRepository.save(state);
        return toAdminState(saved, loadTree(List.of(saved)));
    }

    @Override
    @Transactional
    public void deleteState(Long stateId) {
        EpmGalleryState state = requireState(stateId);
        List<EpmGalleryDistrict> districts = districtRepository.findByStateIdOrderByDisplayOrderAscNameAsc(stateId);
        List<Long> districtIds = districts.stream().map(EpmGalleryDistrict::getId).toList();
        if (!districtIds.isEmpty()) {
            imageRepository.deleteAllInBatch(imageRepository.findByDistrictIdIn(districtIds));
            districtRepository.deleteAllInBatch(districts);
        }
        stateRepository.delete(state);
        stateRepository.flush();
        storage.deleteDirectoryQuietly(storage.stateDir(state));
    }

    @Override
    @Transactional
    public EpmGalleryStateAdminDto setStateCover(Long stateId, MultipartFile file) {
        EpmGalleryState state = requireState(stateId);
        Path dir = storage.stateDir(state);
        String oldCover = state.getCoverFile();
        EpmGalleryStorage.StoredImage stored = storage.store(file, dir, COVER_PREFIX);
        state.setCoverFile(stored.fileName());
        // Touch the row so the cover URL's version changes even when only the file did.
        state.setUpdatedAt(LocalDateTime.now());
        EpmGalleryState saved = stateRepository.saveAndFlush(state);
        if (oldCover != null) {
            storage.deleteQuietly(storage.child(dir, oldCover));
        }
        return toAdminState(saved, loadTree(List.of(saved)));
    }

    @Override
    @Transactional
    public EpmGalleryStateAdminDto removeStateCover(Long stateId) {
        EpmGalleryState state = requireState(stateId);
        String oldCover = state.getCoverFile();
        state.setCoverFile(null);
        state.setUpdatedAt(LocalDateTime.now());
        EpmGalleryState saved = stateRepository.saveAndFlush(state);
        if (oldCover != null) {
            storage.deleteQuietly(storage.child(storage.stateDir(state), oldCover));
        }
        return toAdminState(saved, loadTree(List.of(saved)));
    }

    // =========================================================================================
    // admin: districts
    // =========================================================================================

    @Override
    @Transactional
    public EpmGalleryDistrictAdminDto createDistrict(Long stateId, EpmGalleryRegionRequestDto dto) {
        EpmGalleryState state = requireState(stateId);
        String name = dto.name().trim();
        String slug = requireSlug(name);
        if (districtRepository.existsByStateIdAndSlug(stateId, slug)) {
            throw new ConflictException("\"" + state.getName() + "\" already has a district called \"" + name + "\"");
        }
        EpmGalleryDistrict district = new EpmGalleryDistrict();
        district.setStateId(stateId);
        district.setName(name);
        district.setSlug(slug);
        district.setFolder(uniqueFolder(slug, f -> districtRepository.existsByStateIdAndFolder(stateId, f)
                || Files.exists(storage.stateDir(state).resolve(f))));
        district.setDisplayOrder(dto.displayOrder() != null ? dto.displayOrder() : districtRepository.maxDisplayOrderInState(stateId) + 1);
        EpmGalleryDistrict saved = districtRepository.save(district);
        storage.createDirectories(storage.districtDir(state, saved));
        return toAdminDistrict(state, saved, List.of());
    }

    @Override
    @Transactional
    public EpmGalleryDistrictAdminDto updateDistrict(Long districtId, EpmGalleryRegionRequestDto dto) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        String name = dto.name().trim();
        String slug = requireSlug(name);
        if (!slug.equals(district.getSlug()) && districtRepository.existsByStateIdAndSlug(state.getId(), slug)) {
            throw new ConflictException("\"" + state.getName() + "\" already has a district called \"" + name + "\"");
        }
        district.setName(name);
        district.setSlug(slug);
        if (dto.displayOrder() != null) district.setDisplayOrder(dto.displayOrder());
        EpmGalleryDistrict saved = districtRepository.save(district);
        return toAdminDistrict(state, saved, photosOf(saved.getId()));
    }

    @Override
    @Transactional
    public void deleteDistrict(Long districtId) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        imageRepository.deleteAllInBatch(imageRepository.findByDistrictIdIn(List.of(districtId)));
        districtRepository.delete(district);
        districtRepository.flush();
        storage.deleteDirectoryQuietly(storage.districtDir(state, district));
    }

    // =========================================================================================
    // admin: district photos
    // =========================================================================================

    @Override
    public List<EpmGalleryImageDto> listDistrictPhotos(Long districtId) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        return photosOf(districtId).stream().map(p -> toImageDto(state, district, p)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EpmGalleryImageDto uploadDistrictPhoto(Long districtId, MultipartFile file, String caption) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        Path dir = storage.districtDir(state, district);
        EpmGalleryStorage.StoredImage stored = storage.store(file, dir, null);

        EpmGalleryImage photo = new EpmGalleryImage();
        photo.setBlock(EpmGalleryBlock.EPM_GALLERY.getId());
        photo.setDistrictId(districtId);
        applyStored(photo, stored);
        photo.setCaption(blankToNull(caption));
        photo.setDisplayOrder(imageRepository.maxDisplayOrderInDistrict(districtId) + 1);
        try {
            return toImageDto(state, district, imageRepository.save(photo));
        } catch (RuntimeException e) {
            storage.deleteQuietly(storage.child(dir, stored.fileName()));
            throw e;
        }
    }

    @Override
    @Transactional
    public EpmGalleryImageDto updateDistrictPhoto(Long districtId, Long photoId, EpmGalleryImageUpdateDto dto) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        EpmGalleryImage photo = requirePhoto(districtId, photoId);
        if (dto.caption() != null) photo.setCaption(blankToNull(dto.caption()));
        if (dto.displayOrder() != null) photo.setDisplayOrder(dto.displayOrder());
        return toImageDto(state, district, imageRepository.save(photo));
    }

    @Override
    @Transactional
    public EpmGalleryImageDto replaceDistrictPhoto(Long districtId, Long photoId, MultipartFile file) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        EpmGalleryImage photo = requirePhoto(districtId, photoId);
        Path dir = storage.districtDir(state, district);
        String oldFile = photo.getFileName();

        EpmGalleryStorage.StoredImage stored = storage.store(file, dir, null);
        applyStored(photo, stored);
        EpmGalleryImage saved;
        try {
            saved = imageRepository.saveAndFlush(photo);
        } catch (RuntimeException e) {
            storage.deleteQuietly(storage.child(dir, stored.fileName()));
            throw e;
        }
        storage.deleteQuietly(storage.child(dir, oldFile));
        return toImageDto(state, district, saved);
    }

    @Override
    @Transactional
    public List<EpmGalleryImageDto> reorderDistrictPhotos(Long districtId, List<Long> orderedIds) {
        requireDistrict(districtId);
        Map<Long, EpmGalleryImage> byId = imageRepository.findByDistrictIdOrderByDisplayOrderAscIdAsc(districtId).stream()
                .collect(Collectors.toMap(EpmGalleryImage::getId, Function.identity()));
        if (orderedIds == null || orderedIds.size() != byId.size() || !byId.keySet().equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("The new order must list every photo in this district exactly once");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setDisplayOrder(i);
        }
        imageRepository.saveAll(byId.values());
        return listDistrictPhotos(districtId);
    }

    @Override
    @Transactional
    public void deleteDistrictPhoto(Long districtId, Long photoId) {
        EpmGalleryDistrict district = requireDistrict(districtId);
        EpmGalleryState state = requireState(district.getStateId());
        EpmGalleryImage photo = requirePhoto(districtId, photoId);
        imageRepository.delete(photo);
        imageRepository.flush();
        storage.deleteQuietly(storage.child(storage.districtDir(state, district), photo.getFileName()));
    }

    // =========================================================================================
    // tree building
    // =========================================================================================

    /** The given states with all their districts and photos, loaded in three queries. */
    private record Tree(List<EpmGalleryState> states,
                        Map<Long, List<EpmGalleryDistrict>> districtsByState,
                        Map<Long, List<EpmGalleryImage>> photosByDistrict) {

        List<EpmGalleryDistrict> districtsOf(EpmGalleryState state) {
            return districtsByState.getOrDefault(state.getId(), List.of());
        }

        List<EpmGalleryImage> photosOf(EpmGalleryDistrict district) {
            return photosByDistrict.getOrDefault(district.getId(), List.of());
        }
    }

    private Tree loadTree(List<EpmGalleryState> states) {
        List<Long> stateIds = states.stream().map(EpmGalleryState::getId).toList();
        List<EpmGalleryDistrict> districts = stateIds.isEmpty() ? List.of() : districtRepository.findByStateIdIn(stateIds);
        List<Long> districtIds = districts.stream().map(EpmGalleryDistrict::getId).toList();
        List<EpmGalleryImage> photos = districtIds.isEmpty() ? List.of() : imageRepository.findByDistrictIdIn(districtIds);

        Map<Long, List<EpmGalleryDistrict>> districtsByState = districts.stream()
                .sorted(Comparator.comparingInt(EpmGalleryDistrict::getDisplayOrder).thenComparing(EpmGalleryDistrict::getName))
                .collect(Collectors.groupingBy(EpmGalleryDistrict::getStateId, Collectors.toList()));
        Map<Long, List<EpmGalleryImage>> photosByDistrict = photos.stream()
                .sorted(PHOTO_ORDER)
                .collect(Collectors.groupingBy(EpmGalleryImage::getDistrictId, Collectors.toList()));
        return new Tree(states, districtsByState, photosByDistrict);
    }

    private EpmGalleryStateDto toPublicState(EpmGalleryState state, Tree tree) {
        List<EpmGalleryDistrictDto> districts = tree.districtsOf(state).stream()
                .map(d -> {
                    List<EpmGalleryImage> photos = tree.photosOf(d);
                    String cover = photos.isEmpty() ? null : photoUrl(state, d, photos.get(0));
                    return new EpmGalleryDistrictDto(d.getSlug(), d.getName(), cover, photos.size());
                })
                .filter(d -> d.photoCount() > 0)
                .collect(Collectors.toList());
        int photoCount = districts.stream().mapToInt(EpmGalleryDistrictDto::photoCount).sum();
        String coverUrl = photoCount > 0 || state.getCoverFile() != null ? coverUrl(state, tree) : null;
        return new EpmGalleryStateDto(state.getSlug(), state.getName(), coverUrl, districts.size(), photoCount, districts);
    }

    private EpmGalleryStateAdminDto toAdminState(EpmGalleryState state, Tree tree) {
        List<EpmGalleryDistrictAdminDto> districts = tree.districtsOf(state).stream()
                .map(d -> toAdminDistrict(state, d, tree.photosOf(d)))
                .collect(Collectors.toList());
        int photoCount = districts.stream().mapToInt(EpmGalleryDistrictAdminDto::photoCount).sum();
        boolean anyCover = state.getCoverFile() != null || photoCount > 0;
        return new EpmGalleryStateAdminDto(state.getId(), state.getSlug(), state.getName(), state.getDisplayOrder(),
                anyCover ? coverUrl(state, tree) : null, state.getCoverFile() != null, photoCount, districts);
    }

    private EpmGalleryDistrictAdminDto toAdminDistrict(EpmGalleryState state, EpmGalleryDistrict district,
                                                       List<EpmGalleryImage> photos) {
        String cover = photos.isEmpty() ? null : photoUrl(state, district, photos.get(0));
        return new EpmGalleryDistrictAdminDto(district.getId(), district.getSlug(), district.getName(),
                district.getDisplayOrder(), photos.size(), cover);
    }

    /** Versioned so a new cover (or a new first photo standing in for one) is never served from cache. */
    private String coverUrl(EpmGalleryState state, Tree tree) {
        String version;
        if (state.getCoverFile() != null) {
            version = "c" + epoch(state.getUpdatedAt());
        } else {
            version = tree.districtsOf(state).stream()
                    .flatMap(d -> tree.photosOf(d).stream())
                    .findFirst()
                    .map(p -> "p" + p.getId() + "-" + epoch(p.getUpdatedAt()))
                    .orElse("0");
        }
        return URL_PREFIX + state.getSlug() + "/cover?v=" + version;
    }

    private static String photoUrl(EpmGalleryState state, EpmGalleryDistrict district, EpmGalleryImage photo) {
        return EpmGalleryImageDto.versioned(
                URL_PREFIX + state.getSlug() + "/districts/" + district.getSlug() + "/" + photo.getId() + "/file", photo);
    }

    private EpmGalleryImageDto toImageDto(EpmGalleryState state, EpmGalleryDistrict district, EpmGalleryImage photo) {
        return EpmGalleryImageDto.from(photo,
                URL_PREFIX + state.getSlug() + "/districts/" + district.getSlug() + "/" + photo.getId() + "/file");
    }

    private static long epoch(LocalDateTime time) {
        return time == null ? 0 : time.toEpochSecond(ZoneOffset.UTC);
    }

    // =========================================================================================
    // lookups
    // =========================================================================================

    private List<EpmGalleryImage> photosOf(Long districtId) {
        return imageRepository.findByDistrictIdOrderByDisplayOrderAscIdAsc(districtId);
    }

    private StoredFile loadPhotoFile(EpmGalleryState state, EpmGalleryDistrict district, EpmGalleryImage photo) {
        return storage.load(storage.child(storage.districtDir(state, district), photo.getFileName()), photo.getContentType());
    }

    private EpmGalleryState requireStateBySlug(String slug) {
        return stateRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("State not found: " + slug));
    }

    private EpmGalleryDistrict requireDistrictBySlug(EpmGalleryState state, String slug) {
        return districtRepository.findByStateIdAndSlug(state.getId(), slug)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + slug));
    }

    private EpmGalleryState requireState(Long id) {
        return stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery state not found: " + id));
    }

    private EpmGalleryDistrict requireDistrict(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery district not found: " + id));
    }

    private EpmGalleryImage requirePhoto(Long districtId, Long photoId) {
        return imageRepository.findByIdAndDistrictId(photoId, districtId)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery photo not found: " + photoId));
    }

    private static String requireSlug(String name) {
        String slug = ImageFiles.slugOf(name);
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Name must contain at least one letter or digit");
        }
        return slug;
    }

    /** {@code slug}, or {@code slug-2}, {@code slug-3}... - whichever folder name is still free, both
     * in the database and on disk (a leftover folder is never reused). */
    private static String uniqueFolder(String slug, Function<String, Boolean> taken) {
        String candidate = slug;
        for (int n = 2; taken.apply(candidate); n++) {
            candidate = slug + "-" + n;
        }
        return candidate;
    }
}
