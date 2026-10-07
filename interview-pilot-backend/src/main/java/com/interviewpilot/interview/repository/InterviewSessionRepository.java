package com.interviewpilot.interview.repository;

import com.interviewpilot.interview.entity.InterviewSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);
    boolean existsByJobDescriptionId(Long jobDescriptionId);
    long countByStatus(String status);
    List<InterviewSession> findByJobDescriptionIdAndUserIdOrderByCreatedAtDesc(Long jobDescriptionId, Long userId);
    List<InterviewSession> findTop10ByOrderByUpdatedAtDesc();
    Optional<InterviewSession> findFirstByUserIdAndJobDescriptionIdAndResumeIdAndStatusOrderByCreatedAtDesc(
            Long userId, Long jobDescriptionId, Long resumeId, String status);
}
