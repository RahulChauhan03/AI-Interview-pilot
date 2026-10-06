package com.interviewpilot.application.dto;

import com.interviewpilot.application.document.TailoredResumeContent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TailoredResumeDto {
    private final Long applicationId;
    private final Long baseResumeId;
    private final String baseResumeFileName;
    private final LocalDateTime generatedAt;
    private final TailoredResumeContent content;
    /** Skills the job asks for that are not on the resume and were therefore not added. */
    private final List<String> omittedSkills;
    /** AI suggestions dropped because the resume does not support them. */
    private final int removedSuggestions;
}
