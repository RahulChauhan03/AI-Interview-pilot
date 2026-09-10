package com.interviewpilot.resume.dto;

import org.springframework.web.multipart.MultipartFile;

/** Request wrapper kept at the API boundary for future upload metadata. */
public record ResumeUploadRequestDto(MultipartFile file) { }
