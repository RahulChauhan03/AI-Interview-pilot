package com.interviewpilot.resume.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResumeResponseDto {
    private Long id;
    private Long userId;
    private String originalFileName;
    private String storedFileName;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String storagePath;
    private com.interviewpilot.resume.entity.ResumeStatus status;
    private LocalDateTime uploadTime;
    private LocalDateTime parseTime;
    private Long processingTime;
    private String language;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
