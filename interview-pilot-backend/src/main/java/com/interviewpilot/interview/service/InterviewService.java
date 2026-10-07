package com.interviewpilot.interview.service;

import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewRequestDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import java.util.List;

/** Every method only sees interviews owned by {@code userId}; anything else is "not found". */
public interface InterviewService {
    InterviewResponseDto create(InterviewRequestDto request, Long userId);
    List<InterviewResponseDto> findAllForUser(Long userId);
    InterviewResponseDto findByIdForUser(Long id, Long userId);
    /** The user's interviews for one job description, newest first, including questions and answers. */
    List<InterviewResponseDto> findAllForJob(Long jobDescriptionId, Long userId);
    /** All of the user's interviews including questions and answers, newest first (for aggregate views). */
    List<InterviewResponseDto> findAllWithQuestionsForUser(Long userId);
    /** Deletes one of the user's interviews with its questions and answers; another user's is "not found". */
    void delete(Long id, Long userId);
    InterviewQuestionDto findNextQuestion(Long id, Long userId);
    InterviewAnswerDto submitAnswer(Long sessionId, Long questionId, String answer, Long userId);
    InterviewResponseDto complete(Long sessionId, Long userId);
}
