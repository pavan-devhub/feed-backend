package com.feedstartup.service;

import com.feedstartup.dto.EpmGalleryImageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EpmGalleryService {

    List<EpmGalleryImageDto> list();

    EpmGalleryImageDto upload(MultipartFile file, String caption, String city, String state,
                               boolean featured, Integer displayOrder);

    EpmGalleryImageDto updateMetadata(Long id, String caption, String city, String state,
                                       Boolean featured, Integer displayOrder);

    StoredFile loadImageFile(Long id);

    void delete(Long id);
}
