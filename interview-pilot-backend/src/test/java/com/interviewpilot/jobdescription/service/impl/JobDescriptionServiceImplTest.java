package com.interviewpilot.jobdescription.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.repository.InterviewSessionRepository;
import com.interviewpilot.jobdescription.dto.JobDescriptionRequestDto;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.entity.ResumeJobMatch;
import com.interviewpilot.jobdescription.repository.JobDescriptionRepository;
import com.interviewpilot.jobdescription.repository.ResumeJobMatchRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobDescriptionServiceImplTest {

    private static final long OWNER = 1L;
    private static final long OTHER_USER = 2L;

    @Mock private JobDescriptionRepository jobDescriptionRepository;
    @Mock private ResumeJobMatchRepository matchRepository;
    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResumeService resumeService;
    @Mock private ResumeMatchingService resumeMatchingService;

    private JobDescriptionServiceImpl service;
    private final User owner = User.builder().id(OWNER).build();
    private JobDescription jobDescription;

    @BeforeEach
    void setUp() {
        service = new JobDescriptionServiceImpl(jobDescriptionRepository, matchRepository, interviewSessionRepository,
                userRepository, resumeService, resumeMatchingService, new ObjectMapper());
        jobDescription = JobDescription.builder().id(10L).user(owner)
                .companyName("Acme").jobTitle("Backend Engineer").jobDescription("Java").build();
    }

    @Test
    void createAssignsTheAuthenticatedUserAndTrimsInput() {
        when(userRepository.findById(OWNER)).thenReturn(Optional.of(owner));
        when(jobDescriptionRepository.save(any(JobDescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobDescriptionResponseDto created = service.create(new JobDescriptionRequestDto(" Acme ", " Engineer ", " Java "), OWNER);

        assertThat(created.getUserId()).isEqualTo(OWNER);
        assertThat(created.getCompanyName()).isEqualTo("Acme");
        assertThat(created.getJobTitle()).isEqualTo("Engineer");
    }

    @Test
    void anotherUsersJobDescriptionIsNotFound() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OTHER_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdForUser(10L, OTHER_USER))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Job description not found");
    }

    @Test
    void updateChangesOwnedJobDescription() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OWNER)).thenReturn(Optional.of(jobDescription));
        when(jobDescriptionRepository.save(jobDescription)).thenReturn(jobDescription);

        JobDescriptionResponseDto updated = service.update(10L, new JobDescriptionRequestDto("Acme", "Lead Engineer", "Java, Kafka"), OWNER);

        assertThat(updated.getJobTitle()).isEqualTo("Lead Engineer");
        assertThat(jobDescription.getJobDescription()).isEqualTo("Java, Kafka");
    }

    @Test
    void deleteIsRefusedWhileInterviewsUseTheJobDescription() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OWNER)).thenReturn(Optional.of(jobDescription));
        when(interviewSessionRepository.existsByJobDescriptionId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L, OWNER)).isInstanceOf(ConflictException.class);
        verify(jobDescriptionRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesOwnedJobDescription() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OWNER)).thenReturn(Optional.of(jobDescription));

        service.delete(10L, OWNER);

        verify(jobDescriptionRepository).delete(jobDescription);
    }

    @Test
    void matchWithAnotherUsersJobDescriptionNeverCallsTheAi() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OTHER_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.matchResume(10L, 5L, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(resumeMatchingService, matchRepository);
    }

    @Test
    void matchWithAnotherUsersResumeNeverCallsTheAi() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OWNER)).thenReturn(Optional.of(jobDescription));
        when(resumeService.findParsedByIdForUser(99L, OWNER)).thenThrow(new ResourceNotFoundException("Resume not found"));

        assertThatThrownBy(() -> service.matchResume(10L, 99L, OWNER)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(resumeMatchingService, matchRepository);
    }

    @Test
    void failedAiCallSavesNothing() {
        givenOwnedJobDescriptionAndParsedResume();
        when(resumeMatchingService.analyze("resume text", jobDescription))
                .thenThrow(new OllamaUnavailableException("Ollama is unreachable", null));

        assertThatThrownBy(() -> service.matchResume(10L, 5L, OWNER)).isInstanceOf(OllamaUnavailableException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void successfulMatchIsSavedWithListsAsJson() {
        givenOwnedJobDescriptionAndParsedResume();
        when(resumeMatchingService.analyze("resume text", jobDescription))
                .thenReturn(new ResumeMatchAnalysis(81, List.of("Java"), List.of("Kafka"), List.of("Add a Kafka project")));
        when(matchRepository.save(any(ResumeJobMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeMatchResponseDto match = service.matchResume(10L, 5L, OWNER);

        ArgumentCaptor<ResumeJobMatch> saved = ArgumentCaptor.forClass(ResumeJobMatch.class);
        verify(matchRepository).save(saved.capture());
        assertThat(saved.getValue().getOverallMatchPercentage()).isEqualTo(81.0);
        assertThat(saved.getValue().getMissingSkills()).isEqualTo("[\"Kafka\"]");
        assertThat(match.getMatchScore()).isEqualTo(81.0);
        assertThat(match.getMissingSkills()).containsExactly("Kafka");
        assertThat(match.getResumeFileName()).isEqualTo("cv.pdf");
    }

    @Test
    void unchangedJobDescriptionReusesTheLatestMatchWithoutCallingTheAi() {
        givenOwnedJobDescriptionAndParsedResume();
        jobDescription.setUpdatedAt(LocalDateTime.now().minusHours(2));
        when(matchRepository.findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(5L, 10L))
                .thenReturn(Optional.of(savedMatch(LocalDateTime.now().minusHours(1))));

        ResumeMatchResponseDto match = service.matchResume(10L, 5L, OWNER);

        assertThat(match.isReused()).isTrue();
        assertThat(match.getMatchScore()).isEqualTo(66.0);
        verifyNoInteractions(resumeMatchingService);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void editedJobDescriptionIsMatchedAgain() {
        givenOwnedJobDescriptionAndParsedResume();
        jobDescription.setUpdatedAt(LocalDateTime.now());
        when(matchRepository.findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(5L, 10L))
                .thenReturn(Optional.of(savedMatch(LocalDateTime.now().minusHours(1))));
        when(resumeMatchingService.analyze("resume text", jobDescription))
                .thenReturn(new ResumeMatchAnalysis(81, List.of("Java"), List.of(), List.of()));
        when(matchRepository.save(any(ResumeJobMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeMatchResponseDto match = service.matchResume(10L, 5L, OWNER);

        assertThat(match.isReused()).isFalse();
        assertThat(match.getMatchScore()).isEqualTo(81.0);
    }

    @Test
    void latestMissingSkillsComeFromTheNewestMatch() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OWNER)).thenReturn(Optional.of(jobDescription));
        when(matchRepository.findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(5L, 10L))
                .thenReturn(Optional.of(savedMatch(LocalDateTime.now())));

        assertThat(service.findLatestMissingSkills(10L, 5L, OWNER)).containsExactly("Kafka");
    }

    private ResumeJobMatch savedMatch(LocalDateTime createdAt) {
        ResumeJobMatch match = ResumeJobMatch.builder().id(3L).jobDescription(jobDescription)
                .resume(Resume.builder().id(5L).originalFileName("cv.pdf").build())
                .overallMatchPercentage(66.0).strengths("[\"Java\"]").missingSkills("[\"Kafka\"]").recommendations("[]").build();
        match.setCreatedAt(createdAt);
        return match;
    }

    private void givenOwnedJobDescriptionAndParsedResume() {
        when(jobDescriptionRepository.findByIdAndUserId(10L, OWNER)).thenReturn(Optional.of(jobDescription));
        when(resumeService.findParsedByIdForUser(5L, OWNER)).thenReturn(ParsedResume.builder().cleanText("resume text").build());
        when(resumeService.findByIdForUser(5L, OWNER)).thenReturn(Resume.builder().id(5L).originalFileName("cv.pdf").build());
    }
}
