package com.interviewpilot.application.dto;

import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Everything about one job description in a single response for the application workspace. */
@Getter
@Builder
public class WorkspaceDto {
    private final JobDescriptionResponseDto job;
    /** Null until the user starts an application for this job. */
    private final ApplicationDto application;
    private final ResumeMatchResponseDto latestMatch;
    private final List<InterviewResponseDto> interviews;
    private final SkillGapDto skillGaps;
    /** Topics likely to come up: required skills on the resume, missing skills and earlier question categories. */
    private final List<String> topics;
    /** Questions from the latest practice interview for this job (empty until one exists). */
    private final List<String> recentQuestions;
}
