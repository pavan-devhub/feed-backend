package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An admin-uploaded video on the EPM page - today just the full-width one under the navbar
 * ({@link #HERO}). Only its name and metadata live here; the bytes sit directly in the EPM storage
 * root ({@code epm.storage.video-dir}), beside the gallery/ folder rather than inside it. Until a
 * row exists the page plays its built-in video, and deleting the row goes back to that.
 */
@Entity
@Table(name = "epm_page_videos")
public class EpmPageVideo {

    public static final String HERO = "epm-hero";

    // Which spot on the EPM page the video fills - one row per spot.
    @Id
    @Column(length = 64)
    private String slot;

    // Name of the file on disk, resolved against the EPM storage root.
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 64)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EpmPageVideo() {}

    public EpmPageVideo(String slot) {
        this.slot = slot;
    }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
