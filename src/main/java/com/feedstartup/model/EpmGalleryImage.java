package com.feedstartup.model;

import java.time.LocalDateTime;

/**
 * Metadata for one EPM gallery photo. Persisted as a JSON sidecar file next to the image itself
 * under {@code epm.storage.gallery-dir} (e.g. {@code <uuid>.jpg} + {@code <uuid>.json}) - there is
 * no database row for gallery images. The image's id and file extension come from the filename,
 * not from this class.
 */
public class EpmGalleryImage {

    private String caption;

    private String city;

    private String state;

    // Marks an image for the small "top featured" strip on the gallery page.
    private boolean featured = false;

    private int displayOrder = 0;

    private LocalDateTime createdAt;

    public EpmGalleryImage() {}

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
}
