package com.interviewpilot.interview.service;

import com.interviewpilot.resume.ai.AnswerEvaluation;

/** Scores one interview answer using the AI model. */
public interface AnswerScoringService {
    AnswerEvaluation evaluate(String question, String category, String difficulty, String jobTitle, String answer);
}
