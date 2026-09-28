package com.feedstartup.dto;

import com.feedstartup.model.EpmGalleryBlock;

/** Admin read shape describing one image section, with how many images it currently holds. */
public record EpmGalleryBlockDto(String id, String label, String page, Integer maxImages, String description,
                                 long imageCount) {

    public static EpmGalleryBlockDto from(EpmGalleryBlock block, long imageCount) {
        return new EpmGalleryBlockDto(block.getId(), block.getLabel(), block.getPage().name(), block.getMaxImages(),
                block.getDescription(), imageCount);
    }
}
