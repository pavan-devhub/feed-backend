package com.feedstartup.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public class ErsSubmissionDto {

    @NotEmpty(message = "Answers are required")
    private Map<String, Integer> answers;

    public Map<String, Integer> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Integer> answers) {
        this.answers = answers;
    }
}
