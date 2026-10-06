package com.interviewpilot.resume.service;

import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.resume.ai.ResumeMatchAnalysis;

/** Compares a resume with a job description using the AI model. */
public interface ResumeMatchingService {
    ResumeMatchAnalysis analyze(String resumeText, JobDescription jobDescription);
}
