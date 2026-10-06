package com.interviewpilot.admin.dto;

import lombok.Builder;
import lombok.Getter;

/** System-wide counts for the admin dashboard; all values come straight from the database. */
@Getter
@Builder
public class AdminStatsDto {
    private final long users;
    private final long admins;
    private final long resumes;
    private final long resumesParsed;
    /** UPLOADED or PROCESSING. */
    private final long resumesInProgress;
    private final long resumesFailed;
    private final long jobDescriptions;
    private final long matches;
    private final long interviews;
    private final long interviewsCompleted;
    private final long answers;
}
