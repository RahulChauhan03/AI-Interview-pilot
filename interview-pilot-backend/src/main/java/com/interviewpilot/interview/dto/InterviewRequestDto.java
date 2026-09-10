package com.interviewpilot.interview.dto;

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
public class InterviewRequestDto {
    private Long userId;
    private Long resumeId;
    private Long jobDescriptionId;
    private String status;
}
