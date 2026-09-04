package com.feedstartup.dto;

import com.feedstartup.model.EpmGalleryImage;

/** Public read shape for a gallery photo. The browser never sees {@code imagePath} (a server-side
 * disk path) - only the URL it can stream the bytes from, mirroring PublicationSummaryDto. */
public class EpmGalleryImageDto {

    private Long id;
    private String imageUrl;
    private String caption;
    private String city;
    private String state;
    private boolean featured;
    private Integer displayOrder;

    public static EpmGalleryImageDto from(EpmGalleryImage img) {
        EpmGalleryImageDto dto = new EpmGalleryImageDto();
        dto.id = img.getId();
        dto.imageUrl = "/api/epm/gallery/" + img.getId() + "/file";
        dto.caption = img.getCaption();
        dto.city = img.getCity();
        dto.state = img.getState();
        dto.featured = img.isFeatured();
        dto.displayOrder = img.getDisplayOrder();
        return dto;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getCaption() { return caption; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public boolean isFeatured() { return featured; }
    public Integer getDisplayOrder() { return displayOrder; }
}
