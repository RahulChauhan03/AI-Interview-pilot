package com.interviewpilot.resume.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.GeneratedQuestions;
import com.interviewpilot.resume.ai.OllamaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InterviewGenerationServiceImplTest {

    private final OllamaService ollamaService = mock(OllamaService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InterviewGenerationServiceImpl service =
            new InterviewGenerationServiceImpl(ollamaService, new AiResponseParser(objectMapper), objectMapper, 0.7);
    private final JobDescription jobDescription = JobDescription.builder()
            .jobTitle("Backend Engineer").companyName("Acme").jobDescription("Java and Spring Boot").build();

    @Test
    void normalizesQuestionsAndKeepsAtMostTheRequestedCount() {
        when(ollamaService.generateJson(contains("exactly 2 interview questions"), anyMap(), eq(0.7))).thenReturn("""
                {"questions": [
                  {"question": " How does Spring manage transactions? ", "category": "spring boot", "difficulty": "medium"},
                  {"question": "Tell me about a conflict in your team.", "category": "Behavioral", "difficulty": "EASY"},
                  {"question": "An extra question", "category": "JAVA", "difficulty": "HARD"}
                ]}
                """);

        List<GeneratedQuestions.Question> questions = service.generateQuestions("resume", jobDescription, 2, List.of());

        assertThat(questions).containsExactly(
                new GeneratedQuestions.Question("How does Spring manage transactions?", "SPRING_BOOT", "MEDIUM"),
                new GeneratedQuestions.Question("Tell me about a conflict in your team.", "BEHAVIORAL", "EASY"));
    }

    @Test
    void asksAboutMissingSkillsOnlyWhenThereAreAny() {
        when(ollamaService.generateJson(anyString(), anyMap(), eq(0.7))).thenReturn("""
                {"questions": [{"question": "How would you use Kafka?", "category": "KAFKA", "difficulty": "MEDIUM"}]}
                """);

        service.generateQuestions("resume", jobDescription, 3, List.of("Kafka", "Docker"));
        service.generateQuestions("resume", jobDescription, 3, List.of());

        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(ollamaService, times(2)).generateJson(prompts.capture(), anyMap(), eq(0.7));
        assertThat(prompts.getAllValues().get(0)).contains("ask at least one question about them: Kafka, Docker.");
        assertThat(prompts.getAllValues().get(1)).doesNotContain("ask at least one question about them");
    }

    @Test
    void rejectsEmptyQuestionList() {
        when(ollamaService.generateJson(anyString(), anyMap(), eq(0.7))).thenReturn("{\"questions\": []}");

        assertThatThrownBy(() -> service.generateQuestions("resume", jobDescription, 5, List.of()))
                .isInstanceOf(InvalidAiResponseException.class);
    }

    @Test
    void rejectsUnknownDifficulty() {
        when(ollamaService.generateJson(anyString(), anyMap(), eq(0.7))).thenReturn("""
                {"questions": [{"question": "Explain JPA.", "category": "JAVA", "difficulty": "IMPOSSIBLE"}]}
                """);

        assertThatThrownBy(() -> service.generateQuestions("resume", jobDescription, 5, List.of()))
                .isInstanceOf(InvalidAiResponseException.class).hasMessageContaining("difficulty");
    }

    @Test
    void rejectsBlankQuestion() {
        when(ollamaService.generateJson(anyString(), anyMap(), eq(0.7))).thenReturn("""
                {"questions": [{"question": "  ", "category": "JAVA", "difficulty": "EASY"}]}
                """);

        assertThatThrownBy(() -> service.generateQuestions("resume", jobDescription, 5, List.of()))
                .isInstanceOf(InvalidAiResponseException.class);
    }
}
