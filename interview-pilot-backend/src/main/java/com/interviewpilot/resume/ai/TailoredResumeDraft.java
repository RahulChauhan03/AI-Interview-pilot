package com.interviewpilot.resume.ai;

import java.util.List;

/**
 * Validated AI suggestion for a tailored resume; see resources/ollama/tailored-resume-schema.json. Jobs and projects
 * are referenced by index into the base resume, so the AI cannot add or rename them.
 */
public record TailoredResumeDraft(
        String summary,
        List<String> skills,
        List<ExperienceRewrite> experience,
        List<ProjectRewrite> projects) {

    public record ExperienceRewrite(int index, List<String> bullets) {
    }

    public record ProjectRewrite(int index, String description) {
    }
}
