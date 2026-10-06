package com.interviewpilot.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The owner is always the authenticated user; a user id is never accepted from the client. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewRequestDto {

    @NotNull(message = "Resume is required")
    private Long resumeId;

    @NotNull(message = "Job description is required")
    private Long jobDescriptionId;

    /** Optional, defaults to 5. */
    @Min(value = 3, message = "An interview has at least 3 questions")
    @Max(value = 10, message = "An interview has at most 10 questions")
    private Integer questionCount;
}
