package com.feedstartup.dto;

import java.util.List;

/** Everything the district page shows: breadcrumb names plus the district's photos in display order. */
public record EpmGalleryDistrictDetailDto(String id, String name, String stateId, String stateName, int photoCount,
                                          List<EpmGalleryPhotoDto> photos) {}
