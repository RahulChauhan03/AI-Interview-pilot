package com.interviewpilot.resume.ai;

import java.util.List;

/** Validated AI answer for interview generation; see resources/ollama/interview-questions-schema.json. */
public record GeneratedQuestions(List<Question> questions) {

    public record Question(String question, String category, String difficulty) {
    }
}
