package com.feedstartup.service;

import com.feedstartup.dto.EpmGalleryDistrictAdminDto;
import com.feedstartup.dto.EpmGalleryDistrictDetailDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryImageUpdateDto;
import com.feedstartup.dto.EpmGalleryRegionRequestDto;
import com.feedstartup.dto.EpmGalleryStateAdminDto;
import com.feedstartup.dto.EpmGalleryStateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The state -> district -> photo tree behind the gallery page's state cards, stored in
 * epm_gallery_states / epm_gallery_districts / epm_gallery_images with the files under
 * {@code <gallery-dir>/epm-gallery/<state>/<district>/}. Public reads use URL slugs; admin edits
 * use numeric ids.
 */
public interface EpmGalleryRegionService {

    // --- public ---

    /** Every state that holds at least one photo, in display order. */
    List<EpmGalleryStateDto> listStates();

    EpmGalleryStateDto getState(String stateSlug);

    EpmGalleryDistrictDetailDto getDistrict(String stateSlug, String districtSlug);

    /** The state card's picture: its own cover if set, else its first district's first photo. */
    StoredFile loadStateCover(String stateSlug);

    StoredFile loadPhoto(String stateSlug, String districtSlug, Long photoId);

    // --- admin ---

    /** Every state and district, including empty ones the public page hides. */
    List<EpmGalleryStateAdminDto> adminListStates();

    EpmGalleryStateAdminDto createState(EpmGalleryRegionRequestDto dto);

    EpmGalleryStateAdminDto updateState(Long stateId, EpmGalleryRegionRequestDto dto);

    /** Removes the state with all its districts and photos, rows and files. */
    void deleteState(Long stateId);

    EpmGalleryStateAdminDto setStateCover(Long stateId, MultipartFile file);

    EpmGalleryStateAdminDto removeStateCover(Long stateId);

    EpmGalleryDistrictAdminDto createDistrict(Long stateId, EpmGalleryRegionRequestDto dto);

    EpmGalleryDistrictAdminDto updateDistrict(Long districtId, EpmGalleryRegionRequestDto dto);

    /** Removes the district with all its photos, rows and files. */
    void deleteDistrict(Long districtId);

    List<EpmGalleryImageDto> listDistrictPhotos(Long districtId);

    EpmGalleryImageDto uploadDistrictPhoto(Long districtId, MultipartFile file, String caption);

    EpmGalleryImageDto updateDistrictPhoto(Long districtId, Long photoId, EpmGalleryImageUpdateDto dto);

    EpmGalleryImageDto replaceDistrictPhoto(Long districtId, Long photoId, MultipartFile file);

    List<EpmGalleryImageDto> reorderDistrictPhotos(Long districtId, List<Long> orderedIds);

    void deleteDistrictPhoto(Long districtId, Long photoId);
}
