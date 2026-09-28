package com.feedstartup.dto;

import java.util.List;

/** Admin read shape for a state card, including districts that have no photos yet (which the
 * public page hides). {@code hasCustomCover} is false when the card falls back to a district photo. */
public record EpmGalleryStateAdminDto(Long id, String slug, String name, int displayOrder, String coverUrl,
                                      boolean hasCustomCover, int photoCount,
                                      List<EpmGalleryDistrictAdminDto> districts) {}
