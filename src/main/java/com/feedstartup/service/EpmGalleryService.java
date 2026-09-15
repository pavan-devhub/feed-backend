package com.feedstartup.service;

import com.feedstartup.dto.EpmGalleryImageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EpmGalleryService {

    /** Every photo across every block, e.g. for the homepage teaser slider. */
    List<EpmGalleryImageDto> list();

    /** Just the photos uploaded into one section's folder - see EpmGalleryBlock. */
    List<EpmGalleryImageDto> listByBlock(String block);

    EpmGalleryImageDto upload(String block, MultipartFile file, String caption, String city, String state,
                               boolean featured, Integer displayOrder);

    EpmGalleryImageDto updateMetadata(String block, String id, String caption, String city, String state,
                                       Boolean featured, Integer displayOrder);

    StoredFile loadImageFile(String block, String id);

    void delete(String block, String id);
}
