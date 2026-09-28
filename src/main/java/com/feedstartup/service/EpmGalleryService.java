package com.feedstartup.service;

import com.feedstartup.dto.EpmGalleryBlockDto;
import com.feedstartup.dto.EpmGalleryImageDto;
import com.feedstartup.dto.EpmGalleryImageUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Images for the fixed EpmGalleryBlock sections. Every read goes to epm_gallery_images first and
 * only then to the file each row names - see EpmGalleryStorage for where those files live.
 */
public interface EpmGalleryService {

    /** Every photo in the gallery page's sections (not the EPM page's hero/stats/etc.). */
    List<EpmGalleryImageDto> list();

    /** Just one section's images, in display order - see EpmGalleryBlock. */
    List<EpmGalleryImageDto> listByBlock(String block);

    /** Every block with its label, page, image limit and current image count - for the admin panel. */
    List<EpmGalleryBlockDto> listBlocks();

    EpmGalleryImageDto upload(String block, MultipartFile file, String caption, String city, String state,
                              boolean featured, Integer displayOrder);

    EpmGalleryImageDto updateMetadata(String block, Long id, EpmGalleryImageUpdateDto dto);

    /** Swaps the picture itself, keeping the image's id, caption and position. */
    EpmGalleryImageDto replaceFile(String block, Long id, MultipartFile file);

    /** Sets display order to the position of each id in {@code orderedIds}. */
    List<EpmGalleryImageDto> reorder(String block, List<Long> orderedIds);

    StoredFile loadImageFile(String block, Long id);

    void delete(String block, Long id);
}
