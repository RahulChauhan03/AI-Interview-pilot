package com.interviewpilot.resume.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParsedResumeDto {
    private final Long resumeId;
    private final Map<String, Object> personalInformation;
    private final List<Map<String, Object>> experience;
    private final List<Map<String, Object>> education;
    private final List<String> skills;
    private final List<Map<String, Object>> projects;
    private final List<String> certifications;
    private final List<String> achievements;
    private final List<String> languages;
    private final List<String> softSkills;
    private final List<String> technicalSkills;
    private final List<String> companies;
    private final List<String> designations;
    private final String yearsOfExperience;
    private final String summary;
    private final List<String> keywords;
    private final LocalDateTime createdAt;
}
