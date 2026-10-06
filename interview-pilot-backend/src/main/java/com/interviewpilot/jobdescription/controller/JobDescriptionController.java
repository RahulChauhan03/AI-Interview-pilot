package com.interviewpilot.jobdescription.controller;

import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.jobdescription.dto.JobDescriptionRequestDto;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchRequestDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.security.service.CustomUserDetails;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-descriptions")
@RequiredArgsConstructor
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobDescriptionResponseDto>> create(
            @Valid @RequestBody JobDescriptionRequestDto request, @AuthenticationPrincipal CustomUserDetails user) {
        JobDescriptionResponseDto data = jobDescriptionService.create(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(HttpStatus.CREATED, "Job description created", data));
    }

    @GetMapping
    public ApiResponse<List<JobDescriptionResponseDto>> list(@AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Job descriptions retrieved", jobDescriptionService.findAllForUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobDescriptionResponseDto> get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Job description retrieved", jobDescriptionService.findByIdForUser(id, user.getUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobDescriptionResponseDto> update(@PathVariable Long id, @Valid @RequestBody JobDescriptionRequestDto request,
                                                         @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Job description updated", jobDescriptionService.update(id, request, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        jobDescriptionService.delete(id, user.getUserId());
        return response(HttpStatus.OK, "Job description deleted", null);
    }

    /**
     * Runs the AI comparison synchronously (201); this can take a minute or more with a local model. Returns the
     * previous result instead (200, reused=true) while the job description and resume are unchanged.
     */
    @PostMapping("/{id}/matches")
    public ResponseEntity<ApiResponse<ResumeMatchResponseDto>> match(@PathVariable Long id, @Valid @RequestBody ResumeMatchRequestDto request,
                                                                    @AuthenticationPrincipal CustomUserDetails user) {
        ResumeMatchResponseDto data = jobDescriptionService.matchResume(id, request.getResumeId(), user.getUserId());
        HttpStatus status = data.isReused() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = data.isReused() ? "Previous match result reused" : "Resume match completed";
        return ResponseEntity.status(status).body(response(status, message, data));
    }

    @GetMapping("/{id}/matches")
    public ApiResponse<List<ResumeMatchResponseDto>> matches(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Resume matches retrieved", jobDescriptionService.findMatches(id, user.getUserId()));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
