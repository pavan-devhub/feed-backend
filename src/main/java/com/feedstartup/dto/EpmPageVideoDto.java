package com.feedstartup.dto;

import com.feedstartup.model.EpmPageVideo;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Read shape for an uploaded EPM page video. Like EpmGalleryImageDto, {@code videoUrl} is a
 * server-relative streaming URL carrying a version parameter that changes whenever the admin
 * replaces the video, so a cached copy is never played after a swap. */
public record EpmPageVideoDto(String slot, String videoUrl, String fileName, String contentType, Long fileSize,
                              LocalDateTime updatedAt) {

    public static EpmPageVideoDto from(EpmPageVideo video, String fileUrl) {
        LocalDateTime stamp = video.getUpdatedAt() != null ? video.getUpdatedAt() : video.getCreatedAt();
        String versioned = stamp == null ? fileUrl : fileUrl + "?v=" + stamp.toEpochSecond(ZoneOffset.UTC);
        return new EpmPageVideoDto(video.getSlot(), versioned, video.getFileName(), video.getContentType(),
                video.getFileSize(), stamp);
    }
}
