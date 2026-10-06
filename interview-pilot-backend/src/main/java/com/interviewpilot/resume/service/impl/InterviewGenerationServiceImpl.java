package com.interviewpilot.resume.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.GeneratedQuestions;
import com.interviewpilot.resume.ai.JsonSchemas;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.ai.PromptText;
import com.interviewpilot.resume.service.InterviewGenerationService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InterviewGenerationServiceImpl implements InterviewGenerationService {

    private static final int MAX_TEXT_CHARACTERS = 8000;
    private static final int MAX_CATEGORY_LENGTH = 50;
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final OllamaService ollamaService;
    private final AiResponseParser aiResponseParser;
    private final Map<String, Object> schema;
    private final double temperature;

    public InterviewGenerationServiceImpl(OllamaService ollamaService, AiResponseParser aiResponseParser,
                                          ObjectMapper objectMapper,
                                          @Value("${interview.ai.question-temperature}") double temperature) {
        this.ollamaService = ollamaService;
        this.aiResponseParser = aiResponseParser;
        this.schema = JsonSchemas.load(objectMapper, "ollama/interview-questions-schema.json");
        this.temperature = temperature;
    }

    /** Returns at most {@code questionCount} validated questions; fails if the model produced none. */
    @Override
    public List<GeneratedQuestions.Question> generateQuestions(String resumeText, JobDescription jobDescription, int questionCount,
                                                               List<String> missingSkills) {
        String prompt = buildPrompt(resumeText, jobDescription, questionCount, missingSkills);
        String answer = ollamaService.generateJson(prompt, schema, temperature);
        List<GeneratedQuestions.Question> questions = aiResponseParser.parse(answer, GeneratedQuestions.class).questions();
        if (questions.isEmpty()) {
            throw new InvalidAiResponseException("AI returned no interview questions");
        }
        return questions.stream().limit(questionCount).map(this::validated).toList();
    }

    private GeneratedQuestions.Question validated(GeneratedQuestions.Question question) {
        if (question.question().isBlank() || question.category().isBlank()) {
            throw new InvalidAiResponseException("AI returned an interview question without text or category");
        }
        String difficulty = question.difficulty().trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(difficulty)) {
            throw new InvalidAiResponseException("AI returned an unknown question difficulty");
        }
        String category = question.category().trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        if (category.length() > MAX_CATEGORY_LENGTH) {
            category = category.substring(0, MAX_CATEGORY_LENGTH);
        }
        return new GeneratedQuestions.Question(question.question().trim(), category, difficulty);
    }

    private String buildPrompt(String resumeText, JobDescription jobDescription, int questionCount, List<String> missingSkills) {
        String gaps = missingSkills.isEmpty() ? ""
                : "- The resume does not show these skills the job requires; ask at least one question about them: "
                        + String.join(", ", missingSkills) + ".\n";
        return "You are a senior interviewer preparing a job interview.\n"
                + "Write exactly " + questionCount + " interview questions for the candidate below, who is applying for the job below.\n"
                + "- Mix technical questions about the skills the job requires, questions about the candidate's own "
                + "projects and experience, and at least one behavioural question.\n"
                + "- Match the difficulty to the seniority of the job and the candidate's experience.\n"
                + gaps
                + "- Each question must be one clear question that can be answered in a few minutes, without multiple-choice options.\n"
                + "- category: one short upper-case label such as JAVA, SPRING_BOOT, SQL, SYSTEM_DESIGN, PROJECT_EXPERIENCE or BEHAVIORAL.\n"
                + "- difficulty: EASY, MEDIUM or HARD.\n\n"
                + "Job title: " + jobDescription.getJobTitle() + "\n"
                + "Company: " + jobDescription.getCompanyName() + "\n"
                + "Job description:\n" + PromptText.limit(jobDescription.getJobDescription(), MAX_TEXT_CHARACTERS) + "\n\n"
                + "Resume:\n" + PromptText.limit(resumeText, MAX_TEXT_CHARACTERS);
    }
}
