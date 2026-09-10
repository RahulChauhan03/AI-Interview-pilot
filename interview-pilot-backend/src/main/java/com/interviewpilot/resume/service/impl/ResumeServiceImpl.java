package com.interviewpilot.resume.service.impl;

import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.document.ParsedResumeRepository;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.repository.ResumeRepository;
import com.interviewpilot.resume.service.ResumeProcessingService;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.resume.storage.ResumeStorageService;
import com.interviewpilot.resume.storage.StoredFile;
import com.interviewpilot.resume.validator.ResumeFileValidator;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final ResumeFileValidator fileValidator;
    private final ResumeStorageService storageService;
    private final ResumeProcessingService asyncProcessingService;

    @Override
    public Resume upload(MultipartFile file, Long userId) {
        fileValidator.validate(file);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("Resume upload started for user: {}", userId);
        StoredFile storedFile = storageService.store(file);
        LocalDateTime now = LocalDateTime.now();
        Resume resume = Resume.builder().user(user).originalFileName(storedFile.getOriginalFileName())
                .storedFileName(storedFile.getStoredFileName()).fileExtension(storedFile.getFileExtension())
                .mimeType(storedFile.getMimeType()).fileSize(storedFile.getFileSize()).storagePath(storedFile.getStoragePath())
                .status(ResumeStatus.PROCESSING).uploadTime(now).isDeleted(false).build();
        Resume saved = resumeRepository.save(resume);
        asyncProcessingService.process(saved.getId());
        return saved;
    }

    @Override public List<Resume> findAllForUser(Long userId) { return resumeRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId); }
    @Override public Resume findByIdForUser(Long resumeId, Long userId) { return resumeRepository.findByIdAndUserIdAndIsDeletedFalse(resumeId, userId).orElseThrow(() -> new ResourceNotFoundException("Resume not found")); }
    @Override public ParsedResume findParsedByIdForUser(Long resumeId, Long userId) {
        findByIdForUser(resumeId, userId);
        return parsedResumeRepository.findByResumeId(resumeId).orElseThrow(() -> new ResourceNotFoundException("Parsed resume is not available yet"));
    }
    @Override public void softDelete(Long resumeId, Long userId) { Resume resume = findByIdForUser(resumeId, userId); resume.setIsDeleted(true); resumeRepository.save(resume); }
}
