package com.interviewpilot.interview.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponseDto {
    private Long id;
    private Long userId;
    private Long resumeId;
    private Long jobDescriptionId;
    private String status;
    /** 0-100, set when the interview is completed. */
    private Double overallScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String jobTitle;
    private String companyName;
    private String resumeFileName;
    private int totalQuestions;
    private int answeredQuestions;
    /** First unanswered question while the interview is in progress, otherwise null. */
    private Long nextQuestionId;
    /** Only included when a single interview is requested. */
    private List<InterviewQuestionDto> questions;
}
