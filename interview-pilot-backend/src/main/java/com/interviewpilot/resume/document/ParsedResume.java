package com.interviewpilot.resume.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "parsed_resumes")
public class ParsedResume {

    @Id
    private String id;

    private Long resumeId;
    private String rawText;
    private String cleanText;
    private Object aiResponse;
    private Map<String, Object> personalInformation;
    private List<Map<String, Object>> experience;
    private List<Map<String, Object>> education;
    private List<String> skills;
    private List<Map<String, Object>> projects;
    private List<String> certifications;
    private List<String> achievements;
    private List<String> languages;
    private List<String> softSkills;
    private List<String> technicalSkills;
    private List<String> companies;
    private List<String> designations;
    private String yearsOfExperience;
    private String summary;
    private List<String> keywords;
    private String embeddingStatus;
    private LocalDateTime createdAt;
}
