package com.interviewpilot.jobdescription.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResumeMatchResponseDto {
    private final Long id;
    private final Long resumeId;
    private final String resumeFileName;
    private final Long jobDescriptionId;
    private final String jobTitle;
    private final String companyName;
    /** 0-100. */
    private final Double matchScore;
    /** Requirements of the job that the resume covers well. */
    private final List<String> strengths;
    private final List<String> missingSkills;
    private final List<String> recommendations;
    private final LocalDateTime createdAt;
    /** True when an earlier, still valid result was returned instead of running the AI again. */
    private final boolean reused;
}
