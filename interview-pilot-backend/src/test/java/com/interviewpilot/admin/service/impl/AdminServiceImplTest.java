package com.interviewpilot.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.interviewpilot.admin.dto.ActivityDto;
import com.interviewpilot.admin.dto.ComponentStatusDto;
import com.interviewpilot.common.enums.Role;
import com.interviewpilot.interview.entity.InterviewSession;
import com.interviewpilot.interview.repository.InterviewAnswerRepository;
import com.interviewpilot.interview.repository.InterviewSessionRepository;
import com.interviewpilot.jobdescription.repository.JobDescriptionRepository;
import com.interviewpilot.jobdescription.repository.ResumeJobMatchRepository;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.repository.ResumeRepository;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

class AdminServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ResumeRepository resumeRepository = mock(ResumeRepository.class);
    private final InterviewSessionRepository sessionRepository = mock(InterviewSessionRepository.class);
    private final ResumeJobMatchRepository matchRepository = mock(ResumeJobMatchRepository.class);
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final OllamaService ollamaService = mock(OllamaService.class);
    private final AdminServiceImpl service = new AdminServiceImpl(userRepository, resumeRepository, mock(JobDescriptionRepository.class),
            matchRepository, sessionRepository, mock(InterviewAnswerRepository.class), mongoTemplate, ollamaService,
            CircuitBreakerRegistry.ofDefaults());

    @Test
    void systemStatusReportsEachComponentWithoutLeakingErrors() {
        when(mongoTemplate.executeCommand(any(Document.class)))
                .thenThrow(new DataAccessResourceFailureException("mongodb://secret-host:27017 refused"));
        when(ollamaService.isReachable()).thenReturn(false);
        when(ollamaService.modelName()).thenReturn("llama3.2:1b");

        List<ComponentStatusDto> status = service.systemStatus();

        assertThat(status).extracting(ComponentStatusDto::getName).containsExactly("Backend", "MySQL", "MongoDB", "AI (Ollama)");
        assertThat(status).extracting(ComponentStatusDto::getStatus).containsExactly("UP", "UP", "DOWN", "DOWN");
        assertThat(status.get(3).getDetail()).isEqualTo("Model llama3.2:1b · circuit CLOSED");
        assertThat(status).extracting(ComponentStatusDto::getDetail).noneMatch(detail -> detail.contains("secret-host"));
    }

    @Test
    void activityIsNewestFirstAndContainsNoFileNames() {
        User user = User.builder().id(1L).email("asha@example.com").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now().minusDays(2));
        Resume resume = Resume.builder().id(5L).user(user).originalFileName("Asha_Rao_Resume.pdf")
                .status(ResumeStatus.FAILED).processingTime(1200L).build();
        resume.setUpdatedAt(LocalDateTime.now().minusHours(1));
        InterviewSession session = InterviewSession.builder().id(9L).user(user).status("COMPLETED").overallScore(72.4).build();
        session.setUpdatedAt(LocalDateTime.now());
        when(userRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(user));
        when(resumeRepository.findTop15ByIsDeletedFalseOrderByUpdatedAtDesc()).thenReturn(List.of(resume));
        when(sessionRepository.findTop10ByOrderByUpdatedAtDesc()).thenReturn(List.of(session));
        when(matchRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<ActivityDto> activity = service.recentActivity(10);

        assertThat(activity).extracting(ActivityDto::getType).containsExactly("INTERVIEW", "RESUME", "USER");
        assertThat(activity.get(0).getDescription()).isEqualTo("Interview #9 completed with 72/100");
        assertThat(activity.get(1).getDescription()).isEqualTo("Resume #5 processing failed");
        assertThat(activity.get(1).getDurationMs()).isEqualTo(1200L);
        assertThat(activity).extracting(ActivityDto::getDescription).noneMatch(text -> text.contains("Asha_Rao"));
        assertThat(service.recentActivity(1)).hasSize(1);
    }
}
