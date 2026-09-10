package com.interviewpilot.resume.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.document.ParsedResumeRepository;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.parser.ResumeParserService;
import com.interviewpilot.resume.repository.ResumeRepository;
import com.interviewpilot.resume.service.ResumeProcessingService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAsyncProcessingServiceImpl implements ResumeProcessingService {
    private final ResumeRepository resumeRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final ResumeParserService parserService;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    @Override
    @Async("resumeProcessingExecutor")
    public void process(Long resumeId) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            Resume resume = resumeRepository.findById(resumeId).orElseThrow();
            log.info("Resume processing started: {}", resumeId);
            String rawText = parserService.extractText(Path.of(resume.getStoragePath()));
            String cleanText = parserService.cleanText(rawText);
            log.info("Resume text extracted: {}", resumeId);
            log.info("Ollama request started: {}", resumeId);
            String aiJson = ollamaService.analyzeResume(cleanText);
            log.info(aiJson);
            Map<String, Object> response = objectMapper.readValue(aiJson, new TypeReference<>() { });
            log.info("Ollama response received: {}", resumeId);
            ParsedResume parsed = toDocument(resumeId, rawText, cleanText, aiJson, response);
            parsedResumeRepository.findByResumeId(resumeId).ifPresent(existing -> parsed.setId(existing.getId()));
            parsedResumeRepository.save(parsed);
            log.info("Parsed resume saved to MongoDB: {}", resumeId);
            resume.setStatus(ResumeStatus.PARSED);
            resume.setParseTime(LocalDateTime.now());
            resume.setProcessingTime(Duration.between(startedAt, resume.getParseTime()).toMillis());
            resume.setSummary(asString(response, "summary"));
            resume.setLanguage(asString(response, "language"));
            resumeRepository.save(resume);
            log.info("Resume processing completed: {} in {} ms", resumeId, resume.getProcessingTime());
        } catch (Exception exception) {
            log.error("Resume processing failed: {}", resumeId, exception);
            resumeRepository.findById(resumeId).ifPresent(resume -> {
                resume.setStatus(ResumeStatus.FAILED);
                resume.setParseTime(LocalDateTime.now());
                resume.setProcessingTime(Duration.between(startedAt, resume.getParseTime()).toMillis());
                resumeRepository.save(resume);
            });
        }
    }

   private ParsedResume toDocument(Long resumeId,
                                String rawText,
                                String cleanText,
                                String aiJson,
                                Map<String, Object> data) {

    Map<String, Object> personalInformation =
            asMap(data, "personalInformation", "personal_information", "Personal Information");

    Map<String, Object> experience =
            asMap(data, "experience", "Experience");

    Map<String, Object> skills =
            asMap(data, "skills", "Skills");

    Map<String, Object> devOps =
            asMap(data, "devOps", "DevOps");

    return ParsedResume.builder()
            .resumeId(resumeId)
            .rawText(rawText)
            .cleanText(cleanText)
            .aiResponse(data)

            .personalInformation(personalInformation)

            // Experience
            .experience(asMapList(data, "experience", "Experience"))
            .companies(asStringList(experience,
                    "companies",
                    "previousCompanies",
                    "Previous Companies"))
            .designations(asStringList(experience,
                    "designations",
                    "Designation"))
            .yearsOfExperience(asString(experience,
                    "yearsOfExperience",
                    "totalExperience",
                    "Total Experience"))

            // Education & Projects
            .education(asMapList(data, "education", "Education"))
            .projects(asMapList(data, "projects", "Projects"))

            // Skills
            .skills(asStringList(data, "skills"))
            .technicalSkills(asStringList(
                    skills,
                    "technicalSkills",
                    "Technical Skills"))
            .softSkills(asStringList(
                    skills,
                    "softSkills",
                    "Soft Skills"))

            // Other
            .certifications(asStringList(data, "certifications", "Certifications"))
            .achievements(asStringList(data, "achievements", "Achievements"))
            .languages(asStringList(data, "languages", "Languages"))

            // DevOps tools as keywords
            .keywords(asStringList(devOps, "Tools"))

            .summary(asString(data, "summary", "Summary"))
            .embeddingStatus("PENDING")
            .createdAt(LocalDateTime.now())
            .build();
}

private Object value(Map<String, Object> data, String... names) {

    for (String name : names) {
        if (data.containsKey(name)) {
            return data.get(name);
        }
    }

    for (Map.Entry<String, Object> entry : data.entrySet()) {
        for (String name : names) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
    }

    return null;
}

    private String asString(Map<String, Object> data, String... names) { Object value = value(data, names); return value == null ? null : String.valueOf(value); }
    @SuppressWarnings("unchecked") private Map<String, Object> asMap(Map<String, Object> data, String... names) { Object value = value(data, names); return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Collections.emptyMap(); }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> asMapList(Map<String, Object> data, String... names) { Object value = value(data, names); return value instanceof List<?> list ? list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList() : Collections.emptyList(); }
    private List<String> asStringList(Map<String, Object> data, String... names) { Object value = value(data, names); return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : value == null ? Collections.emptyList() : List.of(String.valueOf(value)); }
}
