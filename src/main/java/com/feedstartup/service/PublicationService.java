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

    /** {@code id} is "{year}-{month}" with a zero-padded month, e.g. "2025-08" - see
     * PublicationServiceImpl. */
    PublicationDetailDto getById(String id);

    PublicationDetailDto getByYearAndMonth(Integer year, Integer month);

    PublicationDetailDto getLatest();

    /**
     * The archive shown in the sidebar: every other issue published in the given anchor year
     * (both earlier and later months), excluding the anchor month itself, ordered January to
     * December and capped at {@code count} results. Never crosses into another year. Months
     * with no uploaded issue are simply omitted rather than padded.
     */
    List<PublicationSummaryDto> getWindow(Integer year, Integer month, int count);

    StoredFile loadPdfFile(String id);

    StoredFile loadThumbnail(String id);

    PublicationDetailDto uploadPublication(MultipartFile file, String title, Integer year, Integer month,
                                            Integer volume, Integer issueNumber);

    PublicationDetailDto updateMetadata(String id, String title, Integer volume, Integer issueNumber);

    void deletePublication(String id);
}
