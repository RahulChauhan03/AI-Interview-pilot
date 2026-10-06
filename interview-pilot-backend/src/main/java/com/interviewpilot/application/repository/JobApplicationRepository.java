package com.interviewpilot.application.repository;

import com.interviewpilot.application.entity.JobApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);
    Optional<JobApplication> findByJobDescriptionIdAndUserId(Long jobDescriptionId, Long userId);
}
