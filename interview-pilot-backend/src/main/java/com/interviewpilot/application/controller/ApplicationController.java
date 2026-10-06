package com.interviewpilot.application.controller;

import com.interviewpilot.application.document.DownloadFile;
import com.interviewpilot.application.dto.ApplicationDto;
import com.interviewpilot.application.dto.ApplicationStatusRequestDto;
import com.interviewpilot.application.dto.CoverLetterDto;
import com.interviewpilot.application.dto.CoverLetterUpdateRequestDto;
import com.interviewpilot.application.dto.CreateApplicationRequestDto;
import com.interviewpilot.application.dto.GenerateDocumentRequestDto;
import com.interviewpilot.application.dto.SkillGapDto;
import com.interviewpilot.application.dto.TailoredResumeDto;
import com.interviewpilot.application.dto.WorkspaceDto;
import com.interviewpilot.application.service.ApplicationDocumentService;
import com.interviewpilot.application.service.ApplicationService;
import com.interviewpilot.application.service.SkillGapService;
import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The job application workspace: one application per job description, its AI documents and their downloads.
 * Generation endpoints call the local model synchronously and can take a minute or more.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationDocumentService documentService;
    private final SkillGapService skillGapService;

    @GetMapping("/job-descriptions/{id}/workspace")
    public ApiResponse<WorkspaceDto> workspace(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response("Workspace retrieved", applicationService.workspace(id, user.getUserId()));
    }

    /** Idempotent: returns the job's existing application, or creates it with status SAVED. */
    @PostMapping("/job-descriptions/{id}/application")
    public ApiResponse<ApplicationDto> createApplication(@PathVariable Long id, @RequestBody(required = false) CreateApplicationRequestDto request,
                                                         @AuthenticationPrincipal CustomUserDetails user) {
        Long resumeId = request == null ? null : request.getResumeId();
        return response("Application ready", applicationService.createForJob(id, resumeId, user.getUserId()));
    }

    @GetMapping("/applications")
    public ApiResponse<List<ApplicationDto>> applications(@AuthenticationPrincipal CustomUserDetails user) {
        return response("Applications retrieved", applicationService.findAllForUser(user.getUserId()));
    }

    @GetMapping("/applications/{id}")
    public ApiResponse<ApplicationDto> application(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response("Application retrieved", applicationService.findByIdForUser(id, user.getUserId()));
    }

    @PatchMapping("/applications/{id}/status")
    public ApiResponse<ApplicationDto> updateStatus(@PathVariable Long id, @Valid @RequestBody ApplicationStatusRequestDto request,
                                                    @AuthenticationPrincipal CustomUserDetails user) {
        return response("Application status updated", applicationService.updateStatus(id, request.getStatus(), user.getUserId()));
    }

    @PostMapping("/applications/{id}/tailored-resume")
    public ApiResponse<TailoredResumeDto> generateTailoredResume(@PathVariable Long id, @RequestBody(required = false) GenerateDocumentRequestDto request,
                                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return response("Tailored resume generated", documentService.generateTailoredResume(id, resumeId(request), user.getUserId()));
    }

    @GetMapping("/applications/{id}/tailored-resume")
    public ApiResponse<TailoredResumeDto> tailoredResume(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response("Tailored resume retrieved", documentService.getTailoredResume(id, user.getUserId()));
    }

    @GetMapping("/applications/{id}/tailored-resume/pdf")
    public ResponseEntity<byte[]> tailoredResumePdf(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return download(documentService.tailoredResumePdf(id, user.getUserId()));
    }

    @PostMapping("/applications/{id}/cover-letter")
    public ApiResponse<CoverLetterDto> generateCoverLetter(@PathVariable Long id, @RequestBody(required = false) GenerateDocumentRequestDto request,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        return response("Cover letter generated", documentService.generateCoverLetter(id, resumeId(request), user.getUserId()));
    }

    @GetMapping("/applications/{id}/cover-letter")
    public ApiResponse<CoverLetterDto> coverLetter(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response("Cover letter retrieved", documentService.getCoverLetter(id, user.getUserId()));
    }

    @PutMapping("/applications/{id}/cover-letter")
    public ApiResponse<CoverLetterDto> updateCoverLetter(@PathVariable Long id, @Valid @RequestBody CoverLetterUpdateRequestDto request,
                                                         @AuthenticationPrincipal CustomUserDetails user) {
        return response("Cover letter saved", documentService.updateCoverLetter(id, request.getParagraphs(), user.getUserId()));
    }

    @GetMapping("/applications/{id}/cover-letter/pdf")
    public ResponseEntity<byte[]> coverLetterPdf(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return download(documentService.coverLetterPdf(id, user.getUserId()));
    }

    @GetMapping("/applications/{id}/package")
    public ResponseEntity<byte[]> applicationPackage(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return download(documentService.applicationPackage(id, user.getUserId()));
    }

    @GetMapping("/skill-gaps")
    public ApiResponse<SkillGapDto> skillGaps(@AuthenticationPrincipal CustomUserDetails user) {
        return response("Skill gaps retrieved", skillGapService.overall(user.getUserId()));
    }

    private static Long resumeId(GenerateDocumentRequestDto request) {
        return request == null ? null : request.getResumeId();
    }

    private static ResponseEntity<byte[]> download(DownloadFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.bytes().length)
                .body(file.bytes());
    }

    private static <T> ApiResponse<T> response(String message, T data) {
        return ApiResponse.<T>builder().status(HttpStatus.OK.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
