package com.interviewpilot.application.dto;

import com.interviewpilot.application.document.CoverLetterContent;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoverLetterDto {
    private final Long applicationId;
    private final Long baseResumeId;
    private final LocalDateTime updatedAt;
    private final CoverLetterContent content;
    /** AI sentences dropped because the resume does not support them. */
    private final int removedSentences;
    /** True once the user has edited the generated text. */
    private final boolean edited;
}
