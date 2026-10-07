package com.interviewpilot.jobdescription.service;

import com.interviewpilot.jobdescription.dto.JobDescriptionRequestDto;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.entity.JobDescription;
import java.util.List;

/** Every method only sees job descriptions owned by {@code userId}; anything else is "not found". */
public interface JobDescriptionService {
    JobDescriptionResponseDto create(JobDescriptionRequestDto request, Long userId);
    List<JobDescriptionResponseDto> findAllForUser(Long userId);
    JobDescriptionResponseDto findByIdForUser(Long id, Long userId);
    JobDescriptionResponseDto update(Long id, JobDescriptionRequestDto request, Long userId);
    void delete(Long id, Long userId);
    ResumeMatchResponseDto matchResume(Long id, Long resumeId, Long userId);
    List<ResumeMatchResponseDto> findMatches(Long id, Long userId);
    /** All matches of the user's job descriptions, newest first. */
    List<ResumeMatchResponseDto> findAllMatchesForUser(Long userId);
    /** Deletes one of the user's matches; another user's match is "not found". */
    void deleteMatch(Long matchId, Long userId);
    ResumeMatchResponseDto findMatchForUser(Long matchId, Long userId);
    /** Latest match for this job description: for the given resume, or for any resume when resumeId is null. */
    java.util.Optional<ResumeMatchResponseDto> findLatestMatch(Long id, Long resumeId, Long userId);
    /** Missing skills from the latest match of this resume and job description, or an empty list. */
    List<String> findLatestMissingSkills(Long id, Long resumeId, Long userId);
    JobDescription findOwned(Long id, Long userId);
}
