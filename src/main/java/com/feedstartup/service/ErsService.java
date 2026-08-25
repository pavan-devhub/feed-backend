package com.feedstartup.service;

import com.feedstartup.dto.ErsStatusDto;

import java.util.Map;
import java.util.Optional;

public interface ErsService {

    ErsStatusDto submitAssessment(Long userId, Map<String, Integer> rawAnswers);

    Optional<ErsStatusDto> getLatestStatus(Long userId);
}
