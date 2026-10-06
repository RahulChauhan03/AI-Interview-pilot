package com.interviewpilot.jobdescription.repository;

import com.interviewpilot.jobdescription.entity.ResumeJobMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeJobMatchRepository extends JpaRepository<ResumeJobMatch, Long> {
    List<ResumeJobMatch> findByJobDescriptionIdOrderByCreatedAtDesc(Long jobDescriptionId);
    Optional<ResumeJobMatch> findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(Long resumeId, Long jobDescriptionId);
    /** Matches are owned through their job description. */
    List<ResumeJobMatch> findByJobDescriptionUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ResumeJobMatch> findByIdAndJobDescriptionUserId(Long id, Long userId);
    List<ResumeJobMatch> findTop10ByOrderByCreatedAtDesc();
    Optional<ResumeJobMatch> findFirstByJobDescriptionIdOrderByCreatedAtDesc(Long jobDescriptionId);
}
