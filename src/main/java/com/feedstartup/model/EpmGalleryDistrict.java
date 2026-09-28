package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A district card inside one EpmGalleryState. Its photos are the EpmGalleryImage rows pointing at
 * this id, stored on disk under {@code <gallery-dir>/epm-gallery/<state folder>/<folder>/}. Same
 * slug/folder split as EpmGalleryState.
 */
@Entity
@Table(name = "epm_gallery_districts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_epm_gallery_districts_state_slug", columnNames = {"state_id", "slug"}),
        @UniqueConstraint(name = "uk_epm_gallery_districts_state_folder", columnNames = {"state_id", "folder"})
})
public class EpmGalleryDistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String folder;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EpmGalleryDistrict() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStateId() { return stateId; }
    public void setStateId(Long stateId) { this.stateId = stateId; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
