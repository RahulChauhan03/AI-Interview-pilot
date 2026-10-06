package com.interviewpilot.resume.ai;

import java.util.List;
import java.util.Map;

/**
 * Validated AI answer. Field names match resources/ollama/resume-analysis-schema.json and the
 * {@link com.interviewpilot.resume.document.ParsedResume} document.
 */
public record ResumeAnalysis(
        Map<String, Object> personalInformation,
        String summary,
        String yearsOfExperience,
        String language,
        List<Map<String, Object>> experience,
        List<Map<String, Object>> education,
        List<Map<String, Object>> projects,
        List<String> skills,
        List<String> technicalSkills,
        List<String> softSkills,
        List<String> certifications,
        List<String> achievements,
        List<String> languages,
        List<String> companies,
        List<String> designations,
        List<String> keywords) {
}
