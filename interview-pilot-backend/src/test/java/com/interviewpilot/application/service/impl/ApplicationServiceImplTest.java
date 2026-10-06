package com.interviewpilot.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewpilot.application.dto.ApplicationDto;
import com.interviewpilot.application.dto.SkillGapDto;
import com.interviewpilot.application.dto.WorkspaceDto;
import com.interviewpilot.application.entity.JobApplication;
import com.interviewpilot.application.repository.ApplicationDocumentRepository;
import com.interviewpilot.application.repository.JobApplicationRepository;
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
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    private static final long OWNER = 1L;
    private static final long OTHER_USER = 2L;

    @Mock private JobApplicationRepository applicationRepository;
    @Mock private ApplicationDocumentRepository documentRepository;
    @Mock private JobDescriptionService jobDescriptionService;
    @Mock private ResumeService resumeService;
    @Mock private InterviewService interviewService;
    @Mock private SkillGapService skillGapService;
    @Mock private UserRepository userRepository;

    private ApplicationServiceImpl service;
    private JobDescription job;

    @BeforeEach
    void setUp() {
        service = new ApplicationServiceImpl(applicationRepository, documentRepository, jobDescriptionService, resumeService,
                interviewService, skillGapService, userRepository);
        job = JobDescription.builder().id(10L).companyName("Acme Fintech").jobTitle("Backend Engineer").jobDescription("Java").build();
        lenient().when(jobDescriptionService.findLatestMatch(eq(10L), any(), eq(OWNER))).thenReturn(Optional.empty());
        lenient().when(interviewService.findAllForJob(10L, OWNER)).thenReturn(List.of());
        lenient().when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createForJobCreatesASavedApplicationOnce() {
        when(jobDescriptionService.findOwned(10L, OWNER)).thenReturn(job);
        when(applicationRepository.findByJobDescriptionIdAndUserId(10L, OWNER)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(OWNER)).thenReturn(User.builder().id(OWNER).build());
        when(resumeService.findByIdForUser(5L, OWNER)).thenReturn(Resume.builder().id(5L).originalFileName("cv.pdf").build());

        ApplicationDto created = service.createForJob(10L, 5L, OWNER);

        assertThat(created.getStatus()).isEqualTo("SAVED");
        assertThat(created.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(created.getResumeFileName()).isEqualTo("cv.pdf");
        assertThat(created.getMatchScore()).isNull();
        verify(applicationRepository).save(any());
    }

    @Test
    void createForJobReturnsTheExistingApplication() {
        JobApplication existing = JobApplication.builder().id(7L).jobDescription(job).status("APPLIED").build();
        when(jobDescriptionService.findOwned(10L, OWNER)).thenReturn(job);
        when(applicationRepository.findByJobDescriptionIdAndUserId(10L, OWNER)).thenReturn(Optional.of(existing));
        when(jobDescriptionService.findLatestMatch(10L, null, OWNER)).thenReturn(Optional.of(
                ResumeMatchResponseDto.builder().id(3L).matchScore(81.0).build()));

        ApplicationDto result = service.createForJob(10L, null, OWNER);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getStatus()).isEqualTo("APPLIED");
        assertThat(result.getMatchScore()).isEqualTo(81.0);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void anotherUsersJobDescriptionCannotGetAnApplication() {
        when(jobDescriptionService.findOwned(10L, OTHER_USER)).thenThrow(new ResourceNotFoundException("Job description not found"));

        assertThatThrownBy(() -> service.createForJob(10L, null, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void anotherUsersApplicationIsNotFound() {
        when(applicationRepository.findByIdAndUserId(7L, OTHER_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdForUser(7L, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus(7L, "APPLIED", OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void markingAsAppliedRecordsTheFirstApplyDate() {
        JobApplication application = JobApplication.builder().id(7L).jobDescription(job).status("PREPARING").build();
        when(applicationRepository.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(application));

        ApplicationDto applied = service.updateStatus(7L, "APPLIED", OWNER);
        LocalDateTime firstAppliedAt = applied.getAppliedAt();
        service.updateStatus(7L, "INTERVIEW", OWNER);
        ApplicationDto again = service.updateStatus(7L, "APPLIED", OWNER);

        assertThat(firstAppliedAt).isNotNull();
        assertThat(again.getAppliedAt()).isEqualTo(firstAppliedAt);
        assertThat(application.getStatus()).isEqualTo("APPLIED");
    }

    @Test
    void recordPreparationLinksTheResumeAndOnlyMovesSavedForward() {
        JobApplication saved = JobApplication.builder().id(7L).jobDescription(job).status("SAVED").build();
        JobApplication applied = JobApplication.builder().id(8L).jobDescription(job).status("APPLIED").build();
        Resume resume = Resume.builder().id(5L).build();
        when(applicationRepository.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(saved));
        when(applicationRepository.findByIdAndUserId(8L, OWNER)).thenReturn(Optional.of(applied));
        when(resumeService.findByIdForUser(5L, OWNER)).thenReturn(resume);

        service.recordPreparation(7L, 5L, OWNER);
        service.recordPreparation(8L, 5L, OWNER);

        assertThat(saved.getStatus()).isEqualTo("PREPARING");
        assertThat(saved.getResume()).isSameAs(resume);
        assertThat(applied.getStatus()).isEqualTo("APPLIED");
    }

    @Test
    void workspaceWithoutApplicationUsesRealDataOnly() {
        JobDescriptionResponseDto jobDto = JobDescriptionResponseDto.builder().id(10L).jobTitle("Backend Engineer").jobDescription("Java").build();
        InterviewResponseDto interview = InterviewResponseDto.builder().id(4L).status("IN_PROGRESS").questions(List.of(
                InterviewQuestionDto.builder().id(1L).question("Explain transactions in Spring.").category("SPRING").build())).build();
        when(jobDescriptionService.findByIdForUser(10L, OWNER)).thenReturn(jobDto);
        when(applicationRepository.findByJobDescriptionIdAndUserId(10L, OWNER)).thenReturn(Optional.empty());
        when(interviewService.findAllForJob(10L, OWNER)).thenReturn(List.of(interview));
        when(skillGapService.forJob(eq(jobDto), isNull(), isNull(), eq(List.of(interview))))
                .thenReturn(new SkillGapDto(List.of(), List.of(), List.of(), List.of()));

        WorkspaceDto workspace = service.workspace(10L, OWNER);

        assertThat(workspace.getApplication()).isNull();
        assertThat(workspace.getLatestMatch()).isNull();
        assertThat(workspace.getTopics()).isEmpty();
        assertThat(workspace.getRecentQuestions()).containsExactly("Explain transactions in Spring.");
        verify(resumeService, never()).findParsedByIdForUser(anyLong(), anyLong());
    }
}
