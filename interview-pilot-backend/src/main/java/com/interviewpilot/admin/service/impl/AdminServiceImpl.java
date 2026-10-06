package com.interviewpilot.admin.service.impl;

import com.interviewpilot.admin.dto.ActivityDto;
import com.interviewpilot.admin.dto.AdminStatsDto;
import com.interviewpilot.admin.dto.ComponentStatusDto;
import com.interviewpilot.admin.service.AdminService;
import com.interviewpilot.common.enums.Role;
import com.interviewpilot.interview.entity.InterviewSession;
import com.interviewpilot.interview.entity.InterviewStatus;
import com.interviewpilot.interview.repository.InterviewAnswerRepository;
import com.interviewpilot.interview.repository.InterviewSessionRepository;
import com.interviewpilot.jobdescription.entity.ResumeJobMatch;
import com.interviewpilot.jobdescription.repository.JobDescriptionRepository;
import com.interviewpilot.jobdescription.repository.ResumeJobMatchRepository;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.repository.ResumeRepository;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeJobMatchRepository matchRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final MongoTemplate mongoTemplate;
    private final OllamaService ollamaService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public AdminStatsDto stats() {
        return AdminStatsDto.builder()
                .users(userRepository.count())
                .admins(userRepository.countByRole(Role.ADMIN))
                .resumes(resumeRepository.countByIsDeletedFalse())
                .resumesParsed(resumeRepository.countByStatusInAndIsDeletedFalse(List.of(ResumeStatus.PARSED)))
                .resumesInProgress(resumeRepository.countByStatusInAndIsDeletedFalse(List.of(ResumeStatus.UPLOADED, ResumeStatus.PROCESSING)))
                .resumesFailed(resumeRepository.countByStatusInAndIsDeletedFalse(List.of(ResumeStatus.FAILED)))
                .jobDescriptions(jobDescriptionRepository.count())
                .matches(matchRepository.count())
                .interviews(sessionRepository.count())
                .interviewsCompleted(sessionRepository.countByStatus(InterviewStatus.COMPLETED.name()))
                .answers(answerRepository.count())
                .build();
    }

    /** Merges the newest users, resumes, matches and interviews into one timeline, newest first. */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityDto> recentActivity(int limit) {
        List<ActivityDto> events = new ArrayList<>();
        userRepository.findTop10ByOrderByCreatedAtDesc().forEach(user -> events.add(userEvent(user)));
        resumeRepository.findTop15ByIsDeletedFalseOrderByUpdatedAtDesc().forEach(resume -> events.add(resumeEvent(resume)));
        matchRepository.findTop10ByOrderByCreatedAtDesc().forEach(match -> events.add(matchEvent(match)));
        sessionRepository.findTop10ByOrderByUpdatedAtDesc().forEach(session -> events.add(interviewEvent(session)));
        return events.stream()
                .filter(event -> event.getTime() != null)
                .sorted(Comparator.comparing(ActivityDto::getTime).reversed())
                .limit(limit)
                .toList();
    }

    /** Each check reports UP or DOWN with a short, safe detail; failures are logged, not returned. */
    @Override
    public List<ComponentStatusDto> systemStatus() {
        List<ComponentStatusDto> components = new ArrayList<>();
        components.add(status("Backend", true, "Spring Boot API"));
        components.add(status("MySQL", check("MySQL", () -> userRepository.count()), "Relational data"));
        components.add(status("MongoDB", check("MongoDB", () -> mongoTemplate.executeCommand(new Document("ping", 1))), "Parsed resumes"));
        String circuit = circuitBreakerRegistry.circuitBreaker("ollama").getState().name();
        components.add(status("AI (Ollama)", ollamaService.isReachable(),
                "Model " + ollamaService.modelName() + " · circuit " + circuit));
        return components;
    }

    private boolean check(String name, Runnable probe) {
        try {
            probe.run();
            return true;
        } catch (RuntimeException exception) {
            log.warn("Health check failed for {}: {}", name, exception.getClass().getSimpleName());
            return false;
        }
    }

    private ComponentStatusDto status(String name, boolean up, String detail) {
        return ComponentStatusDto.builder().name(name).status(up ? "UP" : "DOWN").detail(detail).build();
    }

    private ActivityDto userEvent(User user) {
        return ActivityDto.builder().time(user.getCreatedAt()).type("USER")
                .description("New " + user.getRole().name().toLowerCase() + " account")
                .userEmail(user.getEmail()).status("REGISTERED").build();
    }

    private ActivityDto resumeEvent(Resume resume) {
        String description = switch (resume.getStatus()) {
            case PARSED -> "Resume #" + resume.getId() + " parsed";
            case FAILED -> "Resume #" + resume.getId() + " processing failed";
            case PROCESSING -> "Resume #" + resume.getId() + " is being processed";
            case UPLOADED -> "Resume #" + resume.getId() + " uploaded";
        };
        return ActivityDto.builder().time(resume.getUpdatedAt()).type("RESUME").description(description)
                .userEmail(resume.getUser().getEmail()).status(resume.getStatus().name())
                .durationMs(resume.getProcessingTime()).build();
    }

    private ActivityDto matchEvent(ResumeJobMatch match) {
        return ActivityDto.builder().time(match.getCreatedAt()).type("MATCH")
                .description("Resume match #" + match.getId() + " scored " + Math.round(Objects.requireNonNullElse(match.getOverallMatchPercentage(), 0.0)) + "%")
                .userEmail(match.getJobDescription().getUser().getEmail()).status("COMPLETED").build();
    }

    private ActivityDto interviewEvent(InterviewSession session) {
        boolean completed = InterviewStatus.COMPLETED.name().equals(session.getStatus());
        String description = completed
                ? "Interview #" + session.getId() + " completed with " + Math.round(Objects.requireNonNullElse(session.getOverallScore(), 0.0)) + "/100"
                : "Interview #" + session.getId() + " in progress";
        return ActivityDto.builder().time(session.getUpdatedAt()).type("INTERVIEW").description(description)
                .userEmail(session.getUser().getEmail()).status(session.getStatus()).build();
    }
}
