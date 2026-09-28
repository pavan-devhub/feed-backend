package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One EPM image. Only its name and metadata live here - the bytes stay on disk, under
 * {@code <gallery-dir>/<block>/<fileName>} for a block image, or
 * {@code <gallery-dir>/epm-gallery/<state folder>/<district folder>/<fileName>} for a district
 * photo (see EpmGalleryDistrict). Every public request reads this table first and only then opens
 * the file it names.
 */
@Entity
@Table(name = "epm_gallery_images", indexes = {
        @Index(name = "idx_epm_gallery_images_block", columnList = "block"),
        @Index(name = "idx_epm_gallery_images_district", columnList = "district_id")
})
public class EpmGalleryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One of the EpmGalleryBlock ids. District photos always carry "epm-gallery".
    @Column(nullable = false, length = 64)
    private String block;

    // Set only for photos inside the state -> district tree.
    @Column(name = "district_id")
    private Long districtId;

    // Name of the file on disk, resolved against the block or district folder.
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 64)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    private Integer width;

    private Integer height;

    @Column(length = 500)
    private String caption;

    // Where the photo was taken - shown alongside the caption, unrelated to the district tree.
    private String city;

    private String state;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

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

    public EpmGalleryImage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public Long getDistrictId() { return districtId; }
    public void setDistrictId(Long districtId) { this.districtId = districtId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
