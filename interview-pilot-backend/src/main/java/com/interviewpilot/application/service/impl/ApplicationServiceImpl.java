package com.interviewpilot.application.service.impl;

import com.interviewpilot.application.document.ResumeFacts;
import com.interviewpilot.application.dto.ApplicationDto;
import com.interviewpilot.application.dto.SkillGapDto;
import com.interviewpilot.application.dto.WorkspaceDto;
import com.interviewpilot.application.entity.ApplicationDocument;
import com.interviewpilot.application.entity.ApplicationStatus;
import com.interviewpilot.application.entity.DocumentType;
import com.interviewpilot.application.entity.JobApplication;
import com.interviewpilot.application.repository.ApplicationDocumentRepository;
import com.interviewpilot.application.repository.JobApplicationRepository;
import com.interviewpilot.application.service.ApplicationService;
import com.interviewpilot.application.service.SkillGapService;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.interview.service.InterviewService;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final int MAX_TOPICS = 10;
    private static final int MAX_RECENT_QUESTIONS = 6;

    private final JobApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final JobDescriptionService jobDescriptionService;
    private final ResumeService resumeService;
    private final InterviewService interviewService;
    private final SkillGapService skillGapService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApplicationDto createForJob(Long jobDescriptionId, Long resumeId, Long userId) {
        JobDescription job = jobDescriptionService.findOwned(jobDescriptionId, userId);
        JobApplication application = applicationRepository.findByJobDescriptionIdAndUserId(jobDescriptionId, userId)
                .orElseGet(() -> {
                    log.info("Application created for jobDescriptionId={}", jobDescriptionId);
                    return applicationRepository.save(JobApplication.builder()
                            .user(userRepository.getReferenceById(userId))
                            .jobDescription(job)
                            .status(ApplicationStatus.SAVED.name())
                            .build());
                });
        if (resumeId != null && application.getResume() == null) {
            application.setResume(resumeService.findByIdForUser(resumeId, userId));
        }
        return toDto(application, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDto> findAllForUser(Long userId) {
        return applicationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(app -> toDto(app, userId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDto findByIdForUser(Long id, Long userId) {
        return toDto(findOwned(id, userId), userId);
    }

    @Override
    @Transactional
    public ApplicationDto updateStatus(Long id, String status, Long userId) {
        JobApplication application = findOwned(id, userId);
        ApplicationStatus newStatus = ApplicationStatus.valueOf(status);
        application.setStatus(newStatus.name());
        if (newStatus == ApplicationStatus.APPLIED && application.getAppliedAt() == null) {
            application.setAppliedAt(LocalDateTime.now());
        }
        log.info("Application status changed: applicationId={}, status={}", id, newStatus);
        return toDto(applicationRepository.save(application), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDto workspace(Long jobDescriptionId, Long userId) {
        JobDescriptionResponseDto job = jobDescriptionService.findByIdForUser(jobDescriptionId, userId);
        JobApplication application = applicationRepository.findByJobDescriptionIdAndUserId(jobDescriptionId, userId).orElse(null);
        Long resumeId = application == null || application.getResume() == null ? null : application.getResume().getId();
        ResumeMatchResponseDto match = jobDescriptionService.findLatestMatch(jobDescriptionId, resumeId, userId)
                .or(() -> jobDescriptionService.findLatestMatch(jobDescriptionId, null, userId))
                .orElse(null);
        List<InterviewResponseDto> interviews = interviewService.findAllForJob(jobDescriptionId, userId);
        Long factsResumeId = resumeId != null ? resumeId : match == null ? null : match.getResumeId();
        SkillGapDto skillGaps = skillGapService.forJob(job, facts(factsResumeId, userId), match, interviews);

        Set<String> topics = new LinkedHashSet<>();
        skillGaps.missing().forEach(skill -> topics.add(skill.name()));
        skillGaps.developing().forEach(skill -> topics.add(skill.name()));
        skillGaps.strong().forEach(skill -> topics.add(skill.name()));
        List<String> recentQuestions = interviews.isEmpty() || interviews.get(0).getQuestions() == null ? List.of()
                : interviews.get(0).getQuestions().stream().map(InterviewQuestionDto::getQuestion).limit(MAX_RECENT_QUESTIONS).toList();

        return WorkspaceDto.builder()
                .job(job)
                .application(application == null ? null : toDto(application, userId))
                .latestMatch(match)
                .interviews(interviews)
                .skillGaps(skillGaps)
                .topics(topics.stream().limit(MAX_TOPICS).toList())
                .recentQuestions(recentQuestions)
                .build();
    }

    @Override
    public JobApplication findOwned(Long id, Long userId) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    @Override
    @Transactional
    public void recordPreparation(Long id, Long resumeId, Long userId) {
        JobApplication application = findOwned(id, userId);
        Resume resume = resumeService.findByIdForUser(resumeId, userId);
        application.setResume(resume);
        if (ApplicationStatus.SAVED.name().equals(application.getStatus())) {
            application.setStatus(ApplicationStatus.PREPARING.name());
        }
        applicationRepository.save(application);
    }

    private ResumeFacts facts(Long resumeId, Long userId) {
        if (resumeId == null) return null;
        try {
            return ResumeFacts.from(resumeService.findParsedByIdForUser(resumeId, userId), "");
        } catch (ResourceNotFoundException exception) {
            return null; // deleted or not parsed yet
        }
    }

    private ApplicationDto toDto(JobApplication application, Long userId) {
        JobDescription job = application.getJobDescription();
        Resume resume = application.getResume();
        ResumeMatchResponseDto match = jobDescriptionService
                .findLatestMatch(job.getId(), resume == null ? null : resume.getId(), userId).orElse(null);
        Map<String, ApplicationDocument> documents = application.getId() == null ? Map.of()
                : documentRepository.findByApplicationId(application.getId()).stream()
                        .collect(Collectors.toMap(ApplicationDocument::getDocumentType, Function.identity()));
        List<InterviewResponseDto> interviews = interviewService.findAllForJob(job.getId(), userId);
        InterviewResponseDto latestInterview = interviews.isEmpty() ? null : interviews.get(0);
        return ApplicationDto.builder()
                .id(application.getId())
                .jobDescriptionId(job.getId())
                .jobTitle(job.getJobTitle())
                .companyName(job.getCompanyName())
                .resumeId(resume == null ? null : resume.getId())
                .resumeFileName(resume == null ? null : resume.getOriginalFileName())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .matchScore(match == null ? null : match.getMatchScore())
                .matchId(match == null ? null : match.getId())
                .tailoredResumeAt(updatedAt(documents.get(DocumentType.TAILORED_RESUME.name())))
                .coverLetterAt(updatedAt(documents.get(DocumentType.COVER_LETTER.name())))
                .interviewCount(interviews.size())
                .latestInterviewId(latestInterview == null ? null : latestInterview.getId())
                .latestInterviewStatus(latestInterview == null ? null : latestInterview.getStatus())
                .latestInterviewScore(latestInterview == null ? null : latestInterview.getOverallScore())
                .build();
    }

    private static LocalDateTime updatedAt(ApplicationDocument document) {
        return document == null ? null : (document.getUpdatedAt() != null ? document.getUpdatedAt() : document.getCreatedAt());
    }
}
