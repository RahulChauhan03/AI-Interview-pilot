package com.interviewpilot.resume.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeStorageService {

    StoredFile store(MultipartFile file);
}
