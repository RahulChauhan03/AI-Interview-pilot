package com.interviewpilot.resume.ai;

import java.util.List;

/** Validated AI answer for scoring one interview answer; see resources/ollama/answer-evaluation-schema.json. */
public record AnswerEvaluation(
        /** 0-100. */
        int score,
        /** Is the answer technically right? */
        String correctness,
        /** Does it address what was asked? */
        String relevance,
        String feedback,
        List<String> strengths,
        List<String> improvements) {
}
