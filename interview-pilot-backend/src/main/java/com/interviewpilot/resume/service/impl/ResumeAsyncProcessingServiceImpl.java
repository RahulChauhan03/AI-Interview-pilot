package com.interviewpilot.resume.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.ai.ResumeAnalysis;
import com.interviewpilot.resume.ai.AiResponseParser;
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
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final AiResponseParser aiResponseParser;
    private final ObjectMapper objectMapper;

    @Value("${resume.processing.stale-after-minutes}")
    private long staleAfterMinutes;

    @Override
    @Async("resumeProcessingExecutor")
    public void process(Long resumeId) {
        LocalDateTime startedAt = LocalDateTime.now();
        // Only one worker can claim a resume, so it is never processed twice at the same time.
        if (resumeRepository.claimForProcessing(resumeId, startedAt, startedAt.minusMinutes(staleAfterMinutes)) == 0) {
            log.info("Resume {} is already being processed or does not need processing; skipping", resumeId);
            return;
        }
        try {
            Resume resume = resumeRepository.findById(resumeId).orElseThrow();
            log.info("Resume processing started: {}", resumeId);
            String rawText = parserService.extractText(Path.of(resume.getStoragePath()));
            String cleanText = parserService.cleanText(rawText);
            log.info("Resume text extracted: {}", resumeId);
            log.info("Ollama request started: {}", resumeId);
            ResumeAnalysis analysis = aiResponseParser.parse(ollamaService.analyzeResume(cleanText), ResumeAnalysis.class);
            log.info("Ollama response received and validated: {}", resumeId);
            ParsedResume parsed = toDocument(resumeId, rawText, cleanText, analysis);
            parsedResumeRepository.findByResumeId(resumeId).ifPresent(existing -> parsed.setId(existing.getId()));
            parsedResumeRepository.save(parsed);
            log.info("Parsed resume saved to MongoDB: {}", resumeId);
            resume.setStatus(ResumeStatus.PARSED);
            resume.setParseTime(LocalDateTime.now());
            resume.setProcessingTime(Duration.between(startedAt, resume.getParseTime()).toMillis());
            resume.setSummary(analysis.summary());
            resume.setLanguage(analysis.language());
            resumeRepository.save(resume);
            log.info("Resume processing completed: {} in {} ms", resumeId, resume.getProcessingTime());
        } catch (Exception exception) {
            // Exception messages in this pipeline never contain resume text or the AI response.
            log.error("Resume processing failed: {}", resumeId, exception);
            resumeRepository.findById(resumeId).ifPresent(resume -> {
                resume.setStatus(ResumeStatus.FAILED);
                resume.setParseTime(LocalDateTime.now());
                resume.setProcessingTime(Duration.between(startedAt, resume.getParseTime()).toMillis());
                resumeRepository.save(resume);
            });
        }
    }

    private ParsedResume toDocument(Long resumeId, String rawText, String cleanText, ResumeAnalysis analysis) {
        return ParsedResume.builder()
                .resumeId(resumeId)
                .rawText(rawText)
                .cleanText(cleanText)
                .aiResponse(objectMapper.convertValue(analysis, new TypeReference<Map<String, Object>>() { }))
                .personalInformation(analysis.personalInformation())
                .experience(analysis.experience())
                .education(analysis.education())
                .projects(analysis.projects())
                .skills(analysis.skills())
                .technicalSkills(analysis.technicalSkills())
                .softSkills(analysis.softSkills())
                .certifications(analysis.certifications())
                .achievements(analysis.achievements())
                .languages(analysis.languages())
                .companies(analysis.companies())
                .designations(analysis.designations())
                .yearsOfExperience(analysis.yearsOfExperience())
                .keywords(analysis.keywords())
                .summary(analysis.summary())
                .embeddingStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
