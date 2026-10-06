package com.interviewpilot.resume.service;

import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.repository.ResumeRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Queues resumes again when they were left behind: still UPLOADED because the processing queue was
 * full, or stuck in PROCESSING because the application stopped mid-way. Only records untouched for
 * longer than resume.processing.stale-after-minutes are picked up, and the claim in
 * {@link ResumeProcessingService#process(Long)} prevents a resume from being processed twice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeProcessingRecoveryJob {

    private final ResumeRepository resumeRepository;
    private final ResumeProcessingService processingService;

    @Value("${resume.processing.stale-after-minutes}")
    private long staleAfterMinutes;

    @Value("${resume.ollama.read-timeout}")
    private Duration ollamaReadTimeout;

    /**
     * A resume still being processed must never look stale, or it would be processed a second time
     * in parallel. Processing time is bounded by the Ollama read timeout, so the threshold must exceed it.
     */
    @PostConstruct
    void checkStaleThreshold() {
        if (Duration.ofMinutes(staleAfterMinutes).compareTo(ollamaReadTimeout) <= 0) {
            throw new IllegalStateException("resume.processing.stale-after-minutes (" + staleAfterMinutes
                    + ") must be longer than resume.ollama.read-timeout (" + ollamaReadTimeout + ")");
        }
    }

    @Scheduled(initialDelay = 1,
            fixedDelayString = "${resume.processing.recovery-interval-minutes}",
            timeUnit = TimeUnit.MINUTES)
    public void requeueStaleResumes() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(staleAfterMinutes);
        List<Resume> staleResumes = resumeRepository.findByStatusInAndIsDeletedFalseAndUpdatedAtBefore(
                List.of(ResumeStatus.UPLOADED, ResumeStatus.PROCESSING), staleBefore);
        for (Resume resume : staleResumes) {
            try {
                processingService.process(resume.getId());
                log.info("Re-queued stale resume {} (was {})", resume.getId(), resume.getStatus());
            } catch (TaskRejectedException exception) {
                log.warn("Processing queue is full; remaining stale resumes will be retried on the next run");
                return;
            }
        }
    }
}
