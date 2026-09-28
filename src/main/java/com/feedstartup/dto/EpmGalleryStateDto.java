package com.feedstartup.dto;

import java.util.List;

/** A state card on the EPM gallery page, together with the district cards behind it. */
public record EpmGalleryStateDto(String id, String name, String coverUrl, int districtCount, int photoCount,
                                 List<EpmGalleryDistrictDto> districts) {}
