package com.interviewpilot.application.document;

import java.util.List;

/**
 * A tailored resume as stored and rendered. Contact details, companies, titles, dates, education and
 * certifications always come from the parsed base resume; only wording and ordering come from the AI.
 */
public record TailoredResumeContent(
        String name,
        List<String> contact,
        String summary,
        List<String> skills,
        List<Experience> experience,
        List<Project> projects,
        List<Education> education,
        List<String> certifications) {

    public record Experience(String title, String company, String duration, String location, List<String> bullets) {
    }

    public record Project(String name, String description, List<String> technologies) {
    }

    public record Education(String degree, String institution, String duration, String details) {
    }
}
