package com.feedstartup.dto;

import com.feedstartup.model.ErsAssessment;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Read-only view of a stored ERS assessment attempt, including the sanitized
 * per-question answers so the frontend can rebuild the full results view
 * (dimension breakdown, gap list) without requiring the user to retake the test.
 */
public class ErsStatusDto {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
    private Integer totalScore;
    private Integer maxScore;
    private Double percentage;
    private Boolean passed;
    private Map<String, Integer> answers;
    private LocalDateTime createdAt;

    public static ErsStatusDto from(ErsAssessment a) {
        ErsStatusDto dto = new ErsStatusDto();
        dto.id = a.getId();
        dto.totalScore = a.getTotalScore();
        dto.maxScore = a.getMaxScore();
        dto.percentage = a.getPercentage();
        dto.passed = a.getPassed();
        dto.createdAt = a.getCreatedAt();
        try {
            dto.answers = MAPPER.readValue(a.getAnswersJson(), new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            dto.answers = Map.of();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public Double getPercentage() {
        return percentage;
    }

    public Boolean getPassed() {
        return passed;
    }

    public Map<String, Integer> getAnswers() {
        return answers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
