package com.interviewpilot.jobdescription.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.repository.InterviewSessionRepository;
import com.interviewpilot.jobdescription.dto.JobDescriptionRequestDto;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.entity.ResumeJobMatch;
import com.interviewpilot.jobdescription.repository.JobDescriptionRepository;
import com.interviewpilot.jobdescription.repository.ResumeJobMatchRepository;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.ai.ResumeMatchAnalysis;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.service.ResumeMatchingService;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDescriptionServiceImpl implements JobDescriptionService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeJobMatchRepository matchRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final UserRepository userRepository;
    private final ResumeService resumeService;
    private final ResumeMatchingService resumeMatchingService;
    private final ObjectMapper objectMapper;

    @Override
    public JobDescriptionResponseDto create(JobDescriptionRequestDto request, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JobDescription jobDescription = JobDescription.builder().user(user).build();
        apply(request, jobDescription);
        return toDto(jobDescriptionRepository.save(jobDescription));
    }

    @Override
    public List<JobDescriptionResponseDto> findAllForUser(Long userId) {
        return jobDescriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Override
    public JobDescriptionResponseDto findByIdForUser(Long id, Long userId) {
        return toDto(findOwned(id, userId));
    }

    @Override
    public JobDescriptionResponseDto update(Long id, JobDescriptionRequestDto request, Long userId) {
        JobDescription jobDescription = findOwned(id, userId);
        apply(request, jobDescription);
        return toDto(jobDescriptionRepository.save(jobDescription));
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        JobDescription jobDescription = findOwned(id, userId);
        if (interviewSessionRepository.existsByJobDescriptionId(id)) {
            throw new ConflictException("This job description is used by an interview and cannot be deleted");
        }
        jobDescriptionRepository.delete(jobDescription); // its resume matches are removed with it
    }

    /**
     * Returns the latest saved match while it is still valid; otherwise runs the AI analysis. The AI call
     * happens before anything is saved, so a failed analysis leaves no partial match behind.
     */
    @Override
    public ResumeMatchResponseDto matchResume(Long id, Long resumeId, Long userId) {
        JobDescription jobDescription = findOwned(id, userId);
        ParsedResume parsedResume = resumeService.findParsedByIdForUser(resumeId, userId);
        Resume resume = resumeService.findByIdForUser(resumeId, userId);

        Optional<ResumeJobMatch> latest = matchRepository.findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(resumeId, id);
        if (latest.isPresent() && isStillValid(latest.get(), jobDescription, resume)) {
            log.info("Reusing resume match: jobDescriptionId={}, resumeId={}", id, resumeId);
            return toMatchDto(latest.get(), true);
        }

        ResumeMatchAnalysis analysis = resumeMatchingService.analyze(parsedResume.getCleanText(), jobDescription);

        ResumeJobMatch match = matchRepository.save(ResumeJobMatch.builder()
                .resume(resume)
                .jobDescription(jobDescription)
                .overallMatchPercentage((double) analysis.matchScore())
                .strengths(toJson(analysis.strengths()))
                .missingSkills(toJson(analysis.missingSkills()))
                .recommendations(toJson(analysis.recommendations()))
                .build());
        log.info("Resume match completed: jobDescriptionId={}, resumeId={}, score={}", id, resumeId, analysis.matchScore());
        return toMatchDto(match, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeMatchResponseDto> findMatches(Long id, Long userId) {
        findOwned(id, userId);
        return matchRepository.findByJobDescriptionIdOrderByCreatedAtDesc(id).stream()
                .map(match -> toMatchDto(match, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeMatchResponseDto> findAllMatchesForUser(Long userId) {
        return matchRepository.findByJobDescriptionUserIdOrderByCreatedAtDesc(userId).stream()
                .map(match -> toMatchDto(match, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeMatchResponseDto findMatchForUser(Long matchId, Long userId) {
        return matchRepository.findByIdAndJobDescriptionUserId(matchId, userId)
                .map(match -> toMatchDto(match, false))
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResumeMatchResponseDto> findLatestMatch(Long id, Long resumeId, Long userId) {
        findOwned(id, userId);
        Optional<ResumeJobMatch> match = resumeId == null
                ? matchRepository.findFirstByJobDescriptionIdOrderByCreatedAtDesc(id)
                : matchRepository.findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(resumeId, id);
        return match.map(found -> toMatchDto(found, false));
    }

    @Override
    public List<String> findLatestMissingSkills(Long id, Long resumeId, Long userId) {
        findOwned(id, userId);
        return matchRepository.findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(resumeId, id)
                .map(match -> fromJson(match.getMissingSkills()))
                .orElse(List.of());
    }

    @Override
    public JobDescription findOwned(Long id, Long userId) {
        return jobDescriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job description not found"));
    }

    private void apply(JobDescriptionRequestDto request, JobDescription jobDescription) {
        jobDescription.setCompanyName(request.getCompanyName().trim());
        jobDescription.setJobTitle(request.getJobTitle().trim());
        jobDescription.setJobDescription(request.getJobDescription().trim());
    }

    private JobDescriptionResponseDto toDto(JobDescription jobDescription) {
        return JobDescriptionResponseDto.builder()
                .id(jobDescription.getId())
                .userId(jobDescription.getUser().getId())
                .companyName(jobDescription.getCompanyName())
                .jobTitle(jobDescription.getJobTitle())
                .jobDescription(jobDescription.getJobDescription())
                .createdAt(jobDescription.getCreatedAt())
                .updatedAt(jobDescription.getUpdatedAt())
                .build();
    }

    /** A saved match stays valid until the job description is edited or the resume is parsed again. */
    private boolean isStillValid(ResumeJobMatch match, JobDescription jobDescription, Resume resume) {
        LocalDateTime matchedAt = match.getCreatedAt();
        return matchedAt != null
                && (jobDescription.getUpdatedAt() == null || !jobDescription.getUpdatedAt().isAfter(matchedAt))
                && (resume.getParseTime() == null || !resume.getParseTime().isAfter(matchedAt));
    }

    private ResumeMatchResponseDto toMatchDto(ResumeJobMatch match, boolean reused) {
        return ResumeMatchResponseDto.builder()
                .id(match.getId())
                .resumeId(match.getResume().getId())
                .resumeFileName(match.getResume().getOriginalFileName())
                .jobDescriptionId(match.getJobDescription().getId())
                .jobTitle(match.getJobDescription().getJobTitle())
                .companyName(match.getJobDescription().getCompanyName())
                .matchScore(match.getOverallMatchPercentage())
                .strengths(fromJson(match.getStrengths()))
                .missingSkills(fromJson(match.getMissingSkills()))
                .recommendations(fromJson(match.getRecommendations()))
                .createdAt(match.getCreatedAt())
                .reused(reused)
                .build();
    }

    /** The TEXT columns of resume_job_match hold the lists as JSON arrays. */
    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize match details", exception);
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            return List.of(json);
        }
    }
}
