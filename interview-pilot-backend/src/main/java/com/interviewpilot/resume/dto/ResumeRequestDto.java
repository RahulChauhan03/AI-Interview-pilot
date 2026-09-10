package com.interviewpilot.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeRequestDto {
    private Long userId;
    private String resumeName;
    private String resumeUrl;
    private String parsedText;
    private Long fileSize;
    private String status;
}
