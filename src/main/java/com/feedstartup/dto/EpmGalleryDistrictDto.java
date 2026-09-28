package com.feedstartup.dto;

/** A district card on a state's page. {@code coverUrl} is the district's first photo. */
public record EpmGalleryDistrictDto(String id, String name, String coverUrl, int photoCount) {}
