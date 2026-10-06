package com.interviewpilot.application.service;

import com.interviewpilot.application.document.DownloadFile;
import com.interviewpilot.application.dto.CoverLetterDto;
import com.interviewpilot.application.dto.TailoredResumeDto;
import java.util.List;

/** Tailored resume and cover letter of an application, and their downloads. Ownership is checked on every call. */
public interface ApplicationDocumentService {
    TailoredResumeDto generateTailoredResume(Long applicationId, Long resumeId, Long userId);
    TailoredResumeDto getTailoredResume(Long applicationId, Long userId);
    CoverLetterDto generateCoverLetter(Long applicationId, Long resumeId, Long userId);
    CoverLetterDto getCoverLetter(Long applicationId, Long userId);
    CoverLetterDto updateCoverLetter(Long applicationId, List<String> paragraphs, Long userId);
    DownloadFile tailoredResumePdf(Long applicationId, Long userId);
    DownloadFile coverLetterPdf(Long applicationId, Long userId);
    /** ZIP with the resume and cover letter PDFs that exist, plus the job description. */
    DownloadFile applicationPackage(Long applicationId, Long userId);
}
