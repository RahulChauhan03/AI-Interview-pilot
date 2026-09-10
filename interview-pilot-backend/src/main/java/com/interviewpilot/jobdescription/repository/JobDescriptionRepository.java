package com.interviewpilot.jobdescription.repository;

import com.interviewpilot.jobdescription.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
}
