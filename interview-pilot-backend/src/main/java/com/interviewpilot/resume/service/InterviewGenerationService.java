package com.interviewpilot.resume.service;

import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.resume.ai.GeneratedQuestions;
import java.util.List;

/** Generates interview questions for a resume and a job description using the AI model. */
public interface InterviewGenerationService {
    /** {@code missingSkills}: required skills the resume does not show (from a resume match), may be empty. */
    List<GeneratedQuestions.Question> generateQuestions(String resumeText, JobDescription jobDescription, int questionCount,
                                                        List<String> missingSkills);
}
