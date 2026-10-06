package com.interviewpilot.interview.repository;

import com.interviewpilot.interview.entity.InterviewQuestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    Optional<InterviewQuestion> findByIdAndSessionId(Long id, Long sessionId);
}
