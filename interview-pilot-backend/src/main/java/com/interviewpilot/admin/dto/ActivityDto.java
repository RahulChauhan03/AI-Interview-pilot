package com.interviewpilot.admin.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * One recent event derived from existing records. Contains no resume content, file names, answers,
 * prompts or tokens — only ids, statuses and timings.
 */
@Getter
@Builder
public class ActivityDto {
    private final LocalDateTime time;
    /** USER, RESUME, MATCH or INTERVIEW. */
    private final String type;
    private final String description;
    private final String userEmail;
    private final String status;
    private final Long durationMs;
}
