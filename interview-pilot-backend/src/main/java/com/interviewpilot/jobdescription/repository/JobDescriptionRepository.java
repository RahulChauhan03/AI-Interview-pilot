package com.interviewpilot.jobdescription.repository;

import com.interviewpilot.jobdescription.entity.JobDescription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<JobDescription> findByIdAndUserId(Long id, Long userId);
    List<JobDescription> findByUserIdAndCompanyNameAndJobTitle(Long userId, String companyName, String jobTitle);
}
