package com.interviewpilot.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewQuestionDto {
    private final Long id;
    private final Integer sequenceNumber;
    private final String question;
    private final String category;
    private final String difficulty;
    /** Null until the question is answered. */
    private final InterviewAnswerDto answer;
}
