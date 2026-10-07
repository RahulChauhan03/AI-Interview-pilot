package com.interviewpilot.jobdescription.controller;

import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.security.service.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read access to the user's resume/job matches across all job descriptions (created via /api/job-descriptions/{id}/matches). */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final JobDescriptionService jobDescriptionService;

    @GetMapping
    public ApiResponse<List<ResumeMatchResponseDto>> list(@AuthenticationPrincipal CustomUserDetails user) {
        return response("Resume matches retrieved", jobDescriptionService.findAllMatchesForUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeMatchResponseDto> get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response("Resume match retrieved", jobDescriptionService.findMatchForUser(id, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        jobDescriptionService.deleteMatch(id, user.getUserId());
        return response("Resume match deleted", null);
    }

    private <T> ApiResponse<T> response(String message, T data) {
        return ApiResponse.<T>builder().status(HttpStatus.OK.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
