package com.interviewpilot.interview.dto;

import java.time.LocalDateTime;
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
    private Double overallScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
