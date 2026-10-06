package com.interviewpilot.interview.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewAnswerDto {
    private final Long id;
    private final Long questionId;
    private final String answer;
    /** 0-100. */
    private final Double score;
    private final String correctness;
    private final String relevance;
    private final String feedback;
    private final List<String> strengths;
    private final List<String> improvements;
    private final LocalDateTime createdAt;
}
