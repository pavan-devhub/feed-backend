package com.feedstartup.service.impl;

import com.feedstartup.dto.ErsStatusDto;
import com.feedstartup.model.ErsAssessment;
import com.feedstartup.repository.ErsAssessmentRepository;
import com.feedstartup.service.ErsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ErsServiceImpl implements ErsService {

    // Authoritative per-question max points, mirroring the assessment definition on the frontend.
    // The score is always recomputed here rather than trusting a client-submitted total, so a
    // tampered payload can never inflate the stored pass/fail result.
    private static final Map<String, Integer> MAX_POINTS = Map.ofEntries(
            Map.entry("iec", 8), Map.entry("gst", 6), Map.entry("entity", 6),
            Map.entry("fssai", 6), Map.entry("orgcert", 8), Map.entry("testing", 6),
            Map.entry("pkglabel", 6), Map.entry("pkgmaterial", 5), Map.entry("barcode", 4),
            Map.entry("volume", 6), Map.entry("consistency", 5), Map.entry("storage", 4),
            Map.entry("wcap", 6), Map.entry("ecgc", 5), Map.entry("accounts", 4),
            Map.entry("buyer", 6), Map.entry("pricing", 4),
            Map.entry("docs", 3), Map.entry("training", 2)
    );

    private static final int MAX_TOTAL = MAX_POINTS.values().stream().mapToInt(Integer::intValue).sum();
    private static final double PASS_THRESHOLD_PERCENT = 70.0;

    private final ErsAssessmentRepository ersAssessmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ErsServiceImpl(ErsAssessmentRepository ersAssessmentRepository) {
        this.ersAssessmentRepository = ersAssessmentRepository;
    }

    @Override
    public ErsStatusDto submitAssessment(Long userId, Map<String, Integer> rawAnswers) {
        Map<String, Integer> sanitized = new LinkedHashMap<>();
        int total = 0;
        for (Map.Entry<String, Integer> entry : MAX_POINTS.entrySet()) {
            String qid = entry.getKey();
            int max = entry.getValue();
            int raw = rawAnswers.getOrDefault(qid, 0);
            int clamped = Math.max(0, Math.min(max, raw));
            sanitized.put(qid, clamped);
            total += clamped;
        }

        double percentage = (total * 100.0) / MAX_TOTAL;
        boolean passed = percentage > PASS_THRESHOLD_PERCENT;

        ErsAssessment assessment = new ErsAssessment();
        assessment.setUserId(userId);
        assessment.setTotalScore(total);
        assessment.setMaxScore(MAX_TOTAL);
        assessment.setPercentage(percentage);
        assessment.setPassed(passed);
        assessment.setAnswersJson(writeJson(sanitized));

        ErsAssessment saved = ersAssessmentRepository.save(assessment);
        return ErsStatusDto.from(saved);
    }

    @Override
    public Optional<ErsStatusDto> getLatestStatus(Long userId) {
        return ersAssessmentRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(ErsStatusDto::from);
    }

    private String writeJson(Map<String, Integer> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ERS answers", e);
        }
    }
}
