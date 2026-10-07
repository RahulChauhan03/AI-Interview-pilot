package com.interviewpilot.resume.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.JsonSchemas;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.ai.PromptText;
import com.interviewpilot.resume.ai.ResumeMatchAnalysis;
import com.interviewpilot.resume.service.ResumeMatchingService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ResumeMatchingServiceImpl implements ResumeMatchingService {

    static final int MAX_TEXT_CHARACTERS = 8000;

    private final OllamaService ollamaService;
    private final AiResponseParser aiResponseParser;
    private final Map<String, Object> schema;

    public ResumeMatchingServiceImpl(OllamaService ollamaService, AiResponseParser aiResponseParser, ObjectMapper objectMapper) {
        this.ollamaService = ollamaService;
        this.aiResponseParser = aiResponseParser;
        this.schema = JsonSchemas.load(objectMapper, "ollama/resume-match-schema.json");
    }

    @Override
    public ResumeMatchAnalysis analyze(String resumeText, JobDescription jobDescription) {
        String answer = ollamaService.generateJson(buildPrompt(resumeText, jobDescription), schema);
        ResumeMatchAnalysis analysis = aiResponseParser.parse(answer, ResumeMatchAnalysis.class);
        if (analysis.matchScore() < 0 || analysis.matchScore() > 100) {
            throw new InvalidAiResponseException("AI match score is outside 0-100");
        }
        return analysis;
    }

    private String buildPrompt(String resumeText, JobDescription jobDescription) {
        return "You are an experienced technical recruiter. Compare the candidate's resume with the job description.\n"
                + "strengths: requirements of the job that the resume clearly covers (at most 6).\n"
                + "missingSkills: required skills or experience from the job description that the resume does not show (at most 8).\n"
                + "recommendations: short, concrete suggestions to improve the candidate's fit for this job (at most 5).\n"
                // Scored last, after the model has listed the evidence, so the score follows from it.
                + "matchScore: 0-100, how well the resume meets the job's requirements (skills, experience level, domain), "
                + "consistent with the strengths and missing skills you listed. "
                + "Use 90 or more only when nearly all requirements are clearly met, and below 40 when most key requirements are missing.\n"
                + "Base everything only on the two texts below.\n\n"
                + "Job title: " + jobDescription.getJobTitle() + "\n"
                + "Company: " + jobDescription.getCompanyName() + "\n"
                + "Job description:\n" + PromptText.limit(jobDescription.getJobDescription(), MAX_TEXT_CHARACTERS) + "\n\n"
                + "Resume:\n" + PromptText.limit(resumeText, MAX_TEXT_CHARACTERS);
    }
}
