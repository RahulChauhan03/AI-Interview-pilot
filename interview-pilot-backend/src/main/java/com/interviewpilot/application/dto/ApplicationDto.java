package com.interviewpilot.application.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** An application with the state of everything linked to it (match, documents, interviews). */
@Getter
@Builder
public class ApplicationDto {
    private final Long id;
    private final Long jobDescriptionId;
    private final String jobTitle;
    private final String companyName;
    private final Long resumeId;
    private final String resumeFileName;
    private final String status;
    private final LocalDateTime appliedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    /** Latest match score (0-100) for this job and resume, null when not analysed. */
    private final Double matchScore;
    private final Long matchId;
    private final LocalDateTime tailoredResumeAt;
    private final LocalDateTime coverLetterAt;
    private final int interviewCount;
    private final Long latestInterviewId;
    private final String latestInterviewStatus;
    private final Double latestInterviewScore;
}
