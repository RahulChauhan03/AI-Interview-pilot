package com.interviewpilot.resume.ai;

import java.util.List;

/** Validated AI cover letter body (paragraphs only); see resources/ollama/cover-letter-schema.json. */
public record CoverLetterDraft(List<String> paragraphs) {
}
