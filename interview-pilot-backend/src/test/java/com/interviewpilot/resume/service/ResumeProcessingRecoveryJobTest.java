package com.interviewpilot.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.repository.ResumeRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResumeProcessingRecoveryJobTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ResumeProcessingService processingService;

    private ResumeProcessingRecoveryJob job;

    @BeforeEach
    void setUp() {
        job = new ResumeProcessingRecoveryJob(resumeRepository, processingService);
        ReflectionTestUtils.setField(job, "staleAfterMinutes", 15L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void requeuesOnlyStaleUploadedOrProcessingResumes() {
        when(resumeRepository.findByStatusInAndIsDeletedFalseAndUpdatedAtBefore(anyCollection(), any()))
                .thenReturn(List.of(resume(1L, ResumeStatus.PROCESSING), resume(2L, ResumeStatus.UPLOADED)));

        job.requeueStaleResumes();

        ArgumentCaptor<Collection<ResumeStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<LocalDateTime> staleBefore = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(resumeRepository).findByStatusInAndIsDeletedFalseAndUpdatedAtBefore(statuses.capture(), staleBefore.capture());
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(ResumeStatus.UPLOADED, ResumeStatus.PROCESSING);
        assertThat(Duration.between(staleBefore.getValue(), LocalDateTime.now())).isBetween(
                Duration.ofMinutes(15), Duration.ofMinutes(15).plusSeconds(5));
        verify(processingService).process(1L);
        verify(processingService).process(2L);
    }

    @Test
    void stopsWhenTheQueueIsFull() {
        when(resumeRepository.findByStatusInAndIsDeletedFalseAndUpdatedAtBefore(anyCollection(), any()))
                .thenReturn(List.of(resume(1L, ResumeStatus.PROCESSING), resume(2L, ResumeStatus.PROCESSING),
                        resume(3L, ResumeStatus.PROCESSING)));
        lenient().doThrow(new TaskRejectedException("queue full")).when(processingService).process(2L);

        job.requeueStaleResumes();

        verify(processingService).process(1L);
        verify(processingService, never()).process(3L);
    }

    @Test
    void refusesStaleThresholdThatAnActiveResumeCouldReach() {
        ReflectionTestUtils.setField(job, "staleAfterMinutes", 1L);
        ReflectionTestUtils.setField(job, "ollamaReadTimeout", Duration.ofMinutes(10));

        assertThatThrownBy(job::checkStaleThreshold)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be longer than resume.ollama.read-timeout");
    }

    @Test
    void acceptsDefaultThresholds() {
        ReflectionTestUtils.setField(job, "ollamaReadTimeout", Duration.ofMinutes(10)); // stale-after is 15

        job.checkStaleThreshold();
    }

    private Resume resume(Long id, ResumeStatus status) {
        return Resume.builder().id(id).status(status).isDeleted(false).build();
    }
}
