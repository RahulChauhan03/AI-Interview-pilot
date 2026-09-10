package com.interviewpilot.interview.repository;

import com.interviewpilot.interview.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewRepository extends JpaRepository<InterviewSession, Long> {
}
