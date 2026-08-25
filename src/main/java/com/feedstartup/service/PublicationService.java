package com.feedstartup.service;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PublicationService {

    List<YearSummaryDto> listYears();

    List<PublicationSummaryDto> listByYear(Integer year);

    List<PublicationSummaryDto> search(String query);

    PublicationDetailDto getById(Long id);

    PublicationDetailDto getByYearAndMonth(Integer year, Integer month);

    PublicationDetailDto getLatest();

    StoredFile loadPdfFile(Long id);

    StoredFile loadThumbnail(Long id);

    PublicationDetailDto uploadPublication(MultipartFile file, String title, Integer year, Integer month,
                                            Integer volume, Integer issueNumber);

    PublicationDetailDto updateMetadata(Long id, String title, Integer volume, Integer issueNumber);

    void deletePublication(Long id);
}
