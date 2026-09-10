package com.interviewpilot.interview.repository;

import com.interviewpilot.interview.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
}
