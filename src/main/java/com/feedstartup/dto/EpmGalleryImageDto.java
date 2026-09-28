package com.feedstartup.dto;

import com.feedstartup.model.EpmGalleryImage;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Read shape for one gallery image. The browser never sees the server-side disk path - only the
 * URL it can stream the bytes from, mirroring PublicationSummaryDto. {@code imageUrl} carries a
 * version parameter that changes whenever the file is replaced, so a cached copy is never shown
 * after an admin swaps the picture. */
public record EpmGalleryImageDto(Long id, String block, String imageUrl, String fileName, String caption,
                                 String city, String state, boolean featured, Integer displayOrder,
                                 Integer width, Integer height, Long fileSize, LocalDateTime createdAt) {

    public static EpmGalleryImageDto from(EpmGalleryImage img, String fileUrl) {
        return new EpmGalleryImageDto(img.getId(), img.getBlock(), versioned(fileUrl, img), img.getFileName(),
                img.getCaption(), img.getCity(), img.getState(), img.isFeatured(), img.getDisplayOrder(),
                img.getWidth(), img.getHeight(), img.getFileSize(), img.getCreatedAt());
    }

    public static String versioned(String fileUrl, EpmGalleryImage img) {
        LocalDateTime stamp = img.getUpdatedAt() != null ? img.getUpdatedAt() : img.getCreatedAt();
        return stamp == null ? fileUrl : fileUrl + "?v=" + stamp.toEpochSecond(ZoneOffset.UTC);
    }
}
