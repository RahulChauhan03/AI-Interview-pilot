package com.interviewpilot.resume.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.ai.ResumeMatchAnalysis;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResumeMatchingServiceImplTest {

    private final OllamaService ollamaService = mock(OllamaService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumeMatchingServiceImpl service =
            new ResumeMatchingServiceImpl(ollamaService, new AiResponseParser(objectMapper), objectMapper);
    private final JobDescription jobDescription = JobDescription.builder()
            .jobTitle("Backend Engineer").companyName("Acme").jobDescription("Java, Spring Boot, MySQL").build();

    @Test
    void returnsValidatedAnalysisAndSendsBothTextsWithTheMatchSchema() {
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn("""
                {"matchScore": 72, "strengths": ["Spring Boot"], "missingSkills": ["Kafka"], "recommendations": ["Add metrics"]}
                """);

        ResumeMatchAnalysis analysis = service.analyze("Java developer with Spring Boot", jobDescription);

        assertThat(analysis.matchScore()).isEqualTo(72);
        assertThat(analysis.missingSkills()).containsExactly("Kafka");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> schema = ArgumentCaptor.forClass(Map.class);
        verify(ollamaService).generateJson(prompt.capture(), schema.capture());
        assertThat(prompt.getValue()).contains("Backend Engineer", "Java, Spring Boot, MySQL", "Java developer with Spring Boot");
        assertThat(schema.getValue()).containsKey("properties");
    }

    @Test
    void rejectsScoreOutsideRange() {
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn("""
                {"matchScore": 140, "strengths": [], "missingSkills": [], "recommendations": []}
                """);

        assertThatThrownBy(() -> service.analyze("resume", jobDescription))
                .isInstanceOf(InvalidAiResponseException.class).hasMessageContaining("0-100");
    }

    @Test
    void rejectsIncompleteAnswer() {
        when(ollamaService.generateJson(anyString(), any())).thenReturn("{\"matchScore\": 50}");

        assertThatThrownBy(() -> service.analyze("resume", jobDescription)).isInstanceOf(InvalidAiResponseException.class);
    }
}
