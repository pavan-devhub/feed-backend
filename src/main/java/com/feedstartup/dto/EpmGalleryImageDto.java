package com.feedstartup.dto;

import com.feedstartup.model.EpmGalleryImage;

/** Public read shape for a gallery photo. The browser never sees the server-side disk path -
 * only the URL it can stream the bytes from, mirroring PublicationSummaryDto. {@code id} is the
 * image's filename stem (a UUID) and {@code block} is the EpmGalleryBlock folder it lives in -
 * neither is a database id, see EpmGalleryServiceImpl. */
public class EpmGalleryImageDto {

    private String id;
    private String block;
    private String imageUrl;
    private String caption;
    private String city;
    private String state;
    private boolean featured;
    private Integer displayOrder;

    public static EpmGalleryImageDto from(String block, String id, EpmGalleryImage img) {
        EpmGalleryImageDto dto = new EpmGalleryImageDto();
        dto.id = id;
        dto.block = block;
        dto.imageUrl = "/api/epm/gallery/" + block + "/" + id + "/file";
        dto.caption = img.getCaption();
        dto.city = img.getCity();
        dto.state = img.getState();
        dto.featured = img.isFeatured();
        dto.displayOrder = img.getDisplayOrder();
        return dto;
    }

    public String getId() { return id; }
    public String getBlock() { return block; }
    public String getImageUrl() { return imageUrl; }
    public String getCaption() { return caption; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public boolean isFeatured() { return featured; }
    public Integer getDisplayOrder() { return displayOrder; }
}
