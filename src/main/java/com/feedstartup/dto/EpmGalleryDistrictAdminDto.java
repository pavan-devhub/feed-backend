package com.feedstartup.dto;

/** Admin read shape for a district: numeric id for edits, slug for the public URL. */
public record EpmGalleryDistrictAdminDto(Long id, String slug, String name, int displayOrder, int photoCount,
                                         String coverUrl) {}
