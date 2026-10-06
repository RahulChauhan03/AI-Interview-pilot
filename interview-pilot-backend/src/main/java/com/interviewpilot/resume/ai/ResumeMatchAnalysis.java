package com.interviewpilot.resume.ai;

import java.util.List;

/** Validated AI answer for a resume / job description match; see resources/ollama/resume-match-schema.json. */
public record ResumeMatchAnalysis(
        int matchScore,
        List<String> strengths,
        List<String> missingSkills,
        List<String> recommendations) {
}
