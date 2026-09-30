package com.feedstartup.service;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.model.PublicationLanguage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Every read method only ever returns issues the caller may see: a normal user never gets a
 * month that hasn't started yet, even when the admin has already uploaded it - it is left out of
 * lists and answers 404 when asked for directly. Admins see everything. See
 * {@link PublicationVisibility}.
 */
public interface PublicationService {

    List<YearSummaryDto> listYears();

    /**
     * With no {@code language}: every language edition published in the year - up to three rows
     * per month, newest month first. With a {@code language}: just that language's issues,
     * January to December - one year's shelf on the reader's publications page.
     */
    List<PublicationSummaryDto> listByYear(Integer year, PublicationLanguage language);

    /**
     * Plain title search, except a query recognised as "Month Year" (either token order, full or
     * abbreviated month name, e.g. "August 2025") instead resolves straight to that one issue -
     * the archive search box is meant to jump to a specific issue by date, not just by title.
     */
    List<PublicationSummaryDto> search(String query);

    /** {@code id} is "{year}-{month}-{language}" with a zero-padded month, e.g. "2025-08-ENGLISH"
     * - see PublicationServiceImpl. */
    PublicationDetailDto getById(String id);

    PublicationDetailDto getByYearAndMonth(Integer year, Integer month, PublicationLanguage language);

    PublicationDetailDto getLatest(PublicationLanguage language);

    /**
     * The archive shown in the sidebar: every other issue published in the given anchor year in
     * the given language (both earlier and later months), excluding the anchor month itself,
     * ordered January to December and capped at {@code count} results. Never crosses into another
     * year. Months with no uploaded issue in that language are simply omitted rather than padded.
     */
    List<PublicationSummaryDto> getWindow(Integer year, Integer month, PublicationLanguage language, int count);

    StoredFile loadPdfFile(String id);

    StoredFile loadThumbnail(String id);

    /** The title is always {@code Publication.TITLE} ("Feed World") - it isn't chosen per upload. */
    PublicationDetailDto uploadPublication(MultipartFile file, Integer year, Integer month, PublicationLanguage language);

    /**
     * Swaps the PDF (and regenerates the thumbnail/page count) for an existing publication,
     * leaving its id, title and publishedDate untouched.
     */
    PublicationDetailDto replacePdf(String id, MultipartFile file);

    void deletePublication(String id);
}
