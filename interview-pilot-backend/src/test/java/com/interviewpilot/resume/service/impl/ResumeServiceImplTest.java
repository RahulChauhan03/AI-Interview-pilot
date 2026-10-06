package com.interviewpilot.resume.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewpilot.resume.document.ParsedResumeRepository;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.repository.ResumeRepository;
import com.interviewpilot.resume.service.ResumeProcessingService;
import com.interviewpilot.resume.storage.ResumeStorageService;
import com.interviewpilot.resume.storage.StoredFile;
import com.interviewpilot.resume.validator.ResumeFileValidator;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParsedResumeRepository parsedResumeRepository;
    @Mock
    private ResumeFileValidator fileValidator;
    @Mock
    private ResumeStorageService storageService;
    @Mock
    private ResumeProcessingService processingService;
    @InjectMocks
    private ResumeServiceImpl resumeService;

    private final MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] {1});

    @BeforeEach
    void setUp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(storageService.store(file)).thenReturn(StoredFile.builder().originalFileName("cv.pdf")
                .storedFileName("uuid_cv.pdf").storagePath("/tmp/uuid_cv.pdf").fileExtension("pdf")
                .mimeType("application/pdf").fileSize(1).build());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(9L);
            return resume;
        });
    }

    @Test
    void uploadQueuesProcessingAndStartsAsUploaded() {
        Resume saved = resumeService.upload(file, 1L);

        assertThat(saved.getStatus()).isEqualTo(ResumeStatus.UPLOADED);
        verify(processingService).process(9L);
    }

    @Test
    void fullProcessingQueueDoesNotFailTheUpload() {
        doThrow(new TaskRejectedException("queue full")).when(processingService).process(9L);

        Resume saved = resumeService.upload(file, 1L);

        // Stays UPLOADED so the recovery job can queue it later; the client still gets its 202.
        assertThat(saved.getId()).isEqualTo(9L);
        assertThat(saved.getStatus()).isEqualTo(ResumeStatus.UPLOADED);
    }
}
