package com.interviewpilot.resume.controller;

import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.resume.dto.ParsedResumeDto;
import com.interviewpilot.resume.dto.ResumeResponseDto;
import com.interviewpilot.resume.dto.ResumeSummaryDto;
import com.interviewpilot.resume.dto.ResumeUploadResponseDto;
import com.interviewpilot.resume.mapper.ResumeMapper;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.security.service.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;
    private final ResumeMapper resumeMapper;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeUploadResponseDto>> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal CustomUserDetails user) {
        ResumeUploadResponseDto data = resumeMapper.toUploadResponse(resumeService.upload(file, user.getUserId()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response(HttpStatus.ACCEPTED, "Resume uploaded and processing started", data));
    }

    @GetMapping
    public ApiResponse<List<ResumeSummaryDto>> list(@AuthenticationPrincipal CustomUserDetails user) {
        List<ResumeSummaryDto> data = resumeService.findAllForUser(user.getUserId()).stream().map(resumeMapper::toSummary).toList();
        return response(HttpStatus.OK, "Resumes retrieved", data);
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeResponseDto> get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Resume retrieved", resumeMapper.toResponse(resumeService.findByIdForUser(id, user.getUserId())));
    }

    @GetMapping("/{id}/parsed")
    public ApiResponse<ParsedResumeDto> parsed(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Parsed resume retrieved", resumeMapper.toParsedResponse(resumeService.findParsedByIdForUser(id, user.getUserId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        resumeService.softDelete(id, user.getUserId());
        return ResponseEntity.ok(response(HttpStatus.OK, "Resume deleted", null));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
