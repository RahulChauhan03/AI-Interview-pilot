package com.interviewpilot.resume.repository;

import com.interviewpilot.resume.entity.Resume;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
}
