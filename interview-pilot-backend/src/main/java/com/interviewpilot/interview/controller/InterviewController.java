package com.interviewpilot.interview.controller;

import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.interview.dto.AnswerRequestDto;
import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewRequestDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.interview.service.InterviewService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /** Generates the questions with the AI model synchronously; this can take a minute or more with a local model. */
    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponseDto>> create(@Valid @RequestBody InterviewRequestDto request,
                                                                   @AuthenticationPrincipal CustomUserDetails user) {
        InterviewResponseDto data = interviewService.create(request, user.getUserId());
        if (data.isReused()) {
            return ResponseEntity.ok(response(HttpStatus.OK, "Interview already in progress for this job and resume", data));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response(HttpStatus.CREATED, "Interview created", data));
    }

    @GetMapping
    public ApiResponse<List<InterviewResponseDto>> list(@AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Interviews retrieved", interviewService.findAllForUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewResponseDto> get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Interview retrieved", interviewService.findByIdForUser(id, user.getUserId()));
    }

    /** Resumes an interview: the first unanswered question, or no data when there is none left. */
    @GetMapping("/{id}/next-question")
    public ApiResponse<InterviewQuestionDto> nextQuestion(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        InterviewQuestionDto question = interviewService.findNextQuestion(id, user.getUserId());
        return response(HttpStatus.OK, question == null ? "No unanswered questions" : "Next question retrieved", question);
    }

    @PostMapping("/{id}/questions/{questionId}/answer")
    public ResponseEntity<ApiResponse<InterviewAnswerDto>> answer(@PathVariable Long id, @PathVariable Long questionId,
                                                                 @Valid @RequestBody AnswerRequestDto request,
                                                                 @AuthenticationPrincipal CustomUserDetails user) {
        InterviewAnswerDto data = interviewService.submitAnswer(id, questionId, request.getAnswer(), user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(HttpStatus.CREATED, "Answer scored", data));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<InterviewResponseDto> complete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return response(HttpStatus.OK, "Interview completed", interviewService.complete(id, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        interviewService.delete(id, user.getUserId());
        return response(HttpStatus.OK, "Interview deleted", null);
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
