package com.interviewpilot.application.service;

import com.interviewpilot.application.dto.ApplicationDto;
import com.interviewpilot.application.dto.WorkspaceDto;
import com.interviewpilot.application.entity.JobApplication;
import java.util.List;

/** Every method only sees the user's own applications; anything else is "not found". */
public interface ApplicationService {
    /** Returns the job's application, creating it (status SAVED) the first time. */
    ApplicationDto createForJob(Long jobDescriptionId, Long resumeId, Long userId);
    List<ApplicationDto> findAllForUser(Long userId);
    ApplicationDto findByIdForUser(Long id, Long userId);
    ApplicationDto updateStatus(Long id, String status, Long userId);
    WorkspaceDto workspace(Long jobDescriptionId, Long userId);
    JobApplication findOwned(Long id, Long userId);
    /** Called when a document is generated: links the base resume and moves SAVED to PREPARING. */
    void recordPreparation(Long id, Long resumeId, Long userId);
}
