package com.feedstartup.service.impl;

import com.feedstartup.dto.EpmGalleryDistrictDetailDto;
import com.feedstartup.dto.EpmGalleryStateDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.EpmGalleryDistrict;
import com.feedstartup.model.EpmGalleryImage;
import com.feedstartup.model.EpmGalleryState;
import com.feedstartup.repository.EpmGalleryDistrictRepository;
import com.feedstartup.repository.EpmGalleryImageRepository;
import com.feedstartup.repository.EpmGalleryStateRepository;
import com.feedstartup.service.EpmGalleryStorage;
import com.feedstartup.service.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The public tree is built only from database rows; files are only opened for the one being streamed. */
class EpmGalleryRegionServiceImplTest {

    @TempDir
    Path galleryDir;

    private final EpmGalleryStateRepository states = mock(EpmGalleryStateRepository.class);
    private final EpmGalleryDistrictRepository districts = mock(EpmGalleryDistrictRepository.class);
    private final EpmGalleryImageRepository images = mock(EpmGalleryImageRepository.class);
    private EpmGalleryRegionServiceImpl service;

    private final EpmGalleryState telangana = state(1L, "telangana", "Telangana");
    private final EpmGalleryState goa = state(2L, "goa", "Goa");
    private final EpmGalleryDistrict hyderabad = district(10L, 1L, "hyderabad", "Hyderabad", 0);
    private final EpmGalleryDistrict emptyDistrict = district(11L, 1L, "warangal", "Warangal", 1);
    private final EpmGalleryImage photo1 = photo(100L, 10L, "b.png", 0);
    private final EpmGalleryImage photo2 = photo(101L, 10L, "a.png", 1);

    @BeforeEach
    void setUp() {
        EpmGalleryStorage storage = new EpmGalleryStorage();
        ReflectionTestUtils.setField(storage, "galleryDir", galleryDir.toString());
        service = new EpmGalleryRegionServiceImpl(states, districts, images, storage);

        when(states.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(telangana, goa));
        when(states.findBySlug(any())).thenAnswer(inv -> "telangana".equals(inv.getArgument(0)) ? Optional.of(telangana) : Optional.empty());
        when(districts.findByStateIdIn(anyCollection())).thenReturn(List.of(emptyDistrict, hyderabad));
        when(districts.findByStateIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(hyderabad, emptyDistrict));
        when(districts.findByStateIdAndSlug(1L, "hyderabad")).thenReturn(Optional.of(hyderabad));
        when(images.findByDistrictIdIn(anyCollection())).thenReturn(List.of(photo2, photo1));
        when(images.findByDistrictIdOrderByDisplayOrderAscIdAsc(10L)).thenReturn(List.of(photo1, photo2));
        when(images.findByDistrictIdOrderByDisplayOrderAscIdAsc(11L)).thenReturn(List.of());
        when(images.findByIdAndDistrictId(100L, 10L)).thenReturn(Optional.of(photo1));
    }

    @Test
    void listsOnlyStatesAndDistrictsThatHoldPhotosInDisplayOrder() {
        List<EpmGalleryStateDto> result = service.listStates();

        assertEquals(1, result.size());
        EpmGalleryStateDto state = result.get(0);
        assertEquals("telangana", state.id());
        assertEquals(1, state.districtCount());
        assertEquals(2, state.photoCount());
        assertEquals("hyderabad", state.districts().get(0).id());
        // district cover = its first photo by display order, not by file name
        assertTrue(state.districts().get(0).coverUrl().startsWith("/api/epm/gallery/states/telangana/districts/hyderabad/100/file?v="));
    }

    @Test
    void districtDetailListsPhotosByDisplayOrderWithTheirStoredSize() {
        EpmGalleryDistrictDetailDto detail = service.getDistrict("telangana", "hyderabad");

        assertEquals("Hyderabad", detail.name());
        assertEquals("Telangana", detail.stateName());
        assertEquals(List.of(100L, 101L), detail.photos().stream().map(p -> p.id()).toList());
        assertEquals(640, detail.photos().get(0).width());
    }

    @Test
    void stateCoverPrefersItsOwnCoverThenFallsBackToTheFirstDistrictPhoto() throws IOException {
        Path hyderabadDir = Files.createDirectories(galleryDir.resolve("epm-gallery/telangana/hyderabad"));
        Files.write(hyderabadDir.resolve("b.png"), new byte[]{1});
        assertEquals("b.png", service.loadStateCover("telangana").filename());

        Files.write(galleryDir.resolve("epm-gallery/telangana/cover-x.avif"), new byte[]{1});
        telangana.setCoverFile("cover-x.avif");
        StoredFile cover = service.loadStateCover("telangana");
        assertEquals("cover-x.avif", cover.filename());
        assertEquals("image/avif", cover.contentType());
    }

    @Test
    void unknownNamesAndMissingFilesAreNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.getState("kerala"));
        assertThrows(ResourceNotFoundException.class, () -> service.getDistrict("telangana", "nalgonda"));
        assertThrows(ResourceNotFoundException.class, () -> service.loadPhoto("telangana", "hyderabad", 999L));
        // the row exists but its file doesn't
        assertThrows(ResourceNotFoundException.class, () -> service.loadPhoto("telangana", "hyderabad", 100L));
    }

    // --- fixtures --------------------------------------------------------------------------

    private static EpmGalleryState state(Long id, String slug, String name) {
        EpmGalleryState s = new EpmGalleryState();
        s.setId(id);
        s.setSlug(slug);
        s.setName(name);
        s.setFolder(slug);
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private static EpmGalleryDistrict district(Long id, Long stateId, String slug, String name, int order) {
        EpmGalleryDistrict d = new EpmGalleryDistrict();
        d.setId(id);
        d.setStateId(stateId);
        d.setSlug(slug);
        d.setName(name);
        d.setFolder(slug);
        d.setDisplayOrder(order);
        return d;
    }

    private static EpmGalleryImage photo(Long id, Long districtId, String fileName, int order) {
        EpmGalleryImage p = new EpmGalleryImage();
        p.setId(id);
        p.setBlock("epm-gallery");
        p.setDistrictId(districtId);
        p.setFileName(fileName);
        p.setContentType("image/png");
        p.setDisplayOrder(order);
        p.setWidth(640);
        p.setHeight(480);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }
}
