package com.feedstartup.service;

import com.feedstartup.dto.EpmPageVideoDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * The admin-uploaded video under the navbar at the top of the EPM page. Every read goes to
 * epm_page_videos first and only then to the file its row names in the EPM storage root.
 */
public interface EpmPageVideoService {

    /** The uploaded video, or empty while the page still plays its built-in one. */
    Optional<EpmPageVideoDto> getHeroVideo();

    /** Stores {@code file} as the page's video, replacing (and deleting) any earlier upload. */
    EpmPageVideoDto replaceHeroVideo(MultipartFile file);

    StoredFile loadHeroVideoFile();

    /** Removes the uploaded video - the EPM page goes back to its built-in one. */
    void deleteHeroVideo();
}
