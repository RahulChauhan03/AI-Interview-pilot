package com.interviewpilot.resume.service;

import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.entity.Resume;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    Resume upload(MultipartFile file, Long userId);
    List<Resume> findAllForUser(Long userId);
    Resume findByIdForUser(Long resumeId, Long userId);
    ParsedResume findParsedByIdForUser(Long resumeId, Long userId);
    void softDelete(Long resumeId, Long userId);
}
