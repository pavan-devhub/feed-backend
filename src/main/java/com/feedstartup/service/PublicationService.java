package com.feedstartup.service;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PublicationService {

    List<YearSummaryDto> listYears();

    List<PublicationSummaryDto> listByYear(Integer year);

    /**
     * Plain title search, except a query recognised as "Month Year" (either token order, full or
     * abbreviated month name, e.g. "August 2025") instead resolves straight to that one issue -
     * the archive search box is meant to jump to a specific issue by date, not just by title.
     */
    List<PublicationSummaryDto> search(String query);

    PublicationDetailDto getById(Long id);

    PublicationDetailDto getByYearAndMonth(Integer year, Integer month);

    PublicationDetailDto getLatest();

    /**
     * The rolling archive window shown in the sidebar: up to {@code count} issues after (not
     * including) the given year/month anchor, oldest first, never crossing into the next year -
     * the window is clamped to December of the anchor year even if that means fewer than
     * {@code count} results. Months in the window that have no uploaded issue are simply
     * omitted rather than padded.
     */
    List<PublicationSummaryDto> getWindow(Integer year, Integer month, int count);

    StoredFile loadPdfFile(Long id);

    StoredFile loadThumbnail(Long id);

    PublicationDetailDto uploadPublication(MultipartFile file, String title, Integer year, Integer month,
                                            Integer volume, Integer issueNumber);

    PublicationDetailDto updateMetadata(Long id, String title, Integer volume, Integer issueNumber);

    void deletePublication(Long id);
}
