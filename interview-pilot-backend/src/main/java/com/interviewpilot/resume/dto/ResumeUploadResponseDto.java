package com.interviewpilot.resume.dto;

import com.interviewpilot.resume.entity.ResumeStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResumeUploadResponseDto {
    private final Long id;
    private final String originalFileName;
    private final ResumeStatus status;
    private final LocalDateTime uploadTime;
}
