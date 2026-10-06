package com.interviewpilot.resume.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.OllamaException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.AiResponseParserTest;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.document.ParsedResumeRepository;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import com.interviewpilot.resume.parser.ResumeParserService;
import com.interviewpilot.resume.repository.ResumeRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ResumeAsyncProcessingServiceImplTest {

    private static final long RESUME_ID = 7L;
    private static final String RESUME_TEXT = "Jane Example | jane@example.com | +1 555 0100 | Backend engineer at Acme";

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ParsedResumeRepository parsedResumeRepository;
    @Mock
    private ResumeParserService parserService;
    @Mock
    private OllamaService ollamaService;

    private ResumeAsyncProcessingServiceImpl service;
    private Resume resume;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        service = new ResumeAsyncProcessingServiceImpl(resumeRepository, parsedResumeRepository, parserService,
                ollamaService, new AiResponseParser(objectMapper), objectMapper);
        ReflectionTestUtils.setField(service, "staleAfterMinutes", 15L);
        resume = Resume.builder().id(RESUME_ID).storagePath("/tmp/resume.pdf")
                .status(ResumeStatus.PROCESSING).isDeleted(false).build();
    }

    @Test
    void validAiResponseIsSavedAndResumeBecomesParsed(CapturedOutput output) {
        givenClaimedResumeWithText();
        when(ollamaService.analyzeResume(RESUME_TEXT)).thenReturn(AiResponseParserTest.VALID_RESPONSE);

        service.process(RESUME_ID);

        ArgumentCaptor<ParsedResume> saved = ArgumentCaptor.forClass(ParsedResume.class);
        verify(parsedResumeRepository).save(saved.capture());
        assertThat(saved.getValue().getResumeId()).isEqualTo(RESUME_ID);
        assertThat(saved.getValue().getPersonalInformation()).containsEntry("firstName", "Jane");
        assertThat(saved.getValue().getTechnicalSkills()).containsExactly("Java", "Spring Boot");
        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.PARSED);
        assertThat(resume.getLanguage()).isEqualTo("English");
        assertNoResumeContentIn(output);
    }

    @Test
    void unavailableOllamaMarksResumeFailed(CapturedOutput output) {
        givenClaimedResumeWithText();
        when(ollamaService.analyzeResume(anyString())).thenThrow(new OllamaUnavailableException(
                "Ollama is unreachable", new ResourceAccessException("Connection refused")));

        service.process(RESUME_ID);

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.FAILED);
        verify(parsedResumeRepository, never()).save(any());
        assertThat(output).contains("Resume processing failed: " + RESUME_ID);
        assertNoResumeContentIn(output);
    }

    @Test
    void timedOutOllamaDoesNotLeaveResumeProcessing() {
        givenClaimedResumeWithText();
        when(ollamaService.analyzeResume(anyString())).thenThrow(new OllamaException(
                "Ollama did not return a complete answer (read timeout or dropped connection)", new ResourceAccessException("Request cancelled")));

        service.process(RESUME_ID);

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.FAILED);
        verify(parsedResumeRepository, never()).save(any());
    }

    @Test
    void invalidAiJsonIsRejectedAndResumeIsNotParsed(CapturedOutput output) {
        givenClaimedResumeWithText();
        when(ollamaService.analyzeResume(anyString())).thenReturn("{\"summary\": \"Jane Example, Backend engineer at Acme\"");

        service.process(RESUME_ID);

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.FAILED);
        verify(parsedResumeRepository, never()).save(any());
        assertThat(output).contains("AI response does not match the expected format");
        assertNoResumeContentIn(output);
    }

    @Test
    void resumeAlreadyClaimedByAnotherWorkerIsSkipped() {
        when(resumeRepository.claimForProcessing(eq(RESUME_ID), any(), any())).thenReturn(0);

        service.process(RESUME_ID);

        verify(resumeRepository, never()).findById(any());
        verifyNoInteractions(parserService, ollamaService, parsedResumeRepository);
    }

    private void givenClaimedResumeWithText() {
        when(resumeRepository.claimForProcessing(eq(RESUME_ID), any(), any())).thenReturn(1);
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(parserService.extractText(any())).thenReturn(RESUME_TEXT);
        when(parserService.cleanText(RESUME_TEXT)).thenReturn(RESUME_TEXT);
    }

    private void assertNoResumeContentIn(CapturedOutput output) {
        assertThat(output).doesNotContain("Jane", "jane@example.com", "555 0100", "Backend engineer");
    }
}
