package com.interviewpilot.application.service;

import com.interviewpilot.application.document.ResumeFacts;
import com.interviewpilot.application.dto.SkillGapDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import java.util.List;

public interface SkillGapService {
    /** Skill gaps for one job. {@code resume} and {@code match} may be null when not available yet. */
    SkillGapDto forJob(JobDescriptionResponseDto job, ResumeFacts resume, ResumeMatchResponseDto match, List<InterviewResponseDto> interviews);

    /** Skill gaps across all of the user's job descriptions and interviews. */
    SkillGapDto overall(Long userId);
}
