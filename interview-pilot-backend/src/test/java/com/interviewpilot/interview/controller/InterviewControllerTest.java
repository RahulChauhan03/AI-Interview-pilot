package com.interviewpilot.interview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewpilot.common.enums.Role;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.interview.service.InterviewService;
import com.interviewpilot.security.config.SecurityConfig;
import com.interviewpilot.security.handler.AccessDeniedHandlerImpl;
import com.interviewpilot.security.handler.AuthenticationEntryPointImpl;
import com.interviewpilot.security.jwt.JwtService;
import com.interviewpilot.security.service.CustomUserDetails;
import com.interviewpilot.security.service.CustomUserDetailsService;
import com.interviewpilot.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterviewController.class)
@Import({SecurityConfig.class, AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class})
class InterviewControllerTest {

    private static final CustomUserDetails OWNER = new CustomUserDetails(User.builder().id(1L).firstName("Asha")
            .lastName("Rao").email("asha@example.com").password("x").role(Role.USER).build());

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InterviewService interviewService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void createReturns201WithQuestions() throws Exception {
        when(interviewService.create(any(), eq(1L))).thenReturn(InterviewResponseDto.builder()
                .id(100L).status("IN_PROGRESS").totalQuestions(5).nextQuestionId(201L).questions(List.of()).build());

        mockMvc.perform(post("/api/interviews").with(user(OWNER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":5,\"jobDescriptionId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.nextQuestionId").value(201));
    }

    @Test
    void createValidatesInput() throws Exception {
        mockMvc.perform(post("/api/interviews").with(user(OWNER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":5,\"questionCount\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.validationErrors.jobDescriptionId").value("Job description is required"))
                .andExpect(jsonPath("$.data.validationErrors.questionCount").value("An interview has at most 10 questions"));
        verifyNoInteractions(interviewService);
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/interviews/100")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/interviews/100/questions/201/answer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"x\"}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(interviewService);
    }

    @Test
    void someoneElsesInterviewReturns404() throws Exception {
        when(interviewService.findByIdForUser(100L, 1L)).thenThrow(new ResourceNotFoundException("Interview not found"));

        mockMvc.perform(get("/api/interviews/100").with(user(OWNER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Interview not found"));
    }

    @Test
    void nextQuestionReturnsTheQuestionOrNoData() throws Exception {
        when(interviewService.findNextQuestion(100L, 1L)).thenReturn(InterviewQuestionDto.builder().id(202L).sequenceNumber(2).build());
        when(interviewService.findNextQuestion(101L, 1L)).thenReturn(null);

        mockMvc.perform(get("/api/interviews/100/next-question").with(user(OWNER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(202));
        mockMvc.perform(get("/api/interviews/101/next-question").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("No unanswered questions"));
    }

    @Test
    void answerReturns201WithScoreAndFeedback() throws Exception {
        when(interviewService.submitAnswer(100L, 201L, "JPA maps objects to tables", 1L)).thenReturn(InterviewAnswerDto.builder()
                .id(300L).questionId(201L).score(70.0).feedback("Good").strengths(List.of("Clear")).improvements(List.of()).build());

        mockMvc.perform(post("/api/interviews/100/questions/201/answer").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"JPA maps objects to tables\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score").value(70.0))
                .andExpect(jsonPath("$.data.feedback").value("Good"));
    }

    @Test
    void blankAnswerReturns400() throws Exception {
        mockMvc.perform(post("/api/interviews/100/questions/201/answer").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.validationErrors.answer").value("Answer is required"));
    }

    @Test
    void answeringFinishedInterviewReturns409() throws Exception {
        when(interviewService.submitAnswer(100L, 201L, "late", 1L)).thenThrow(new ConflictException("This interview is already finished"));

        mockMvc.perform(post("/api/interviews/100/questions/201/answer").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"late\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This interview is already finished"));
    }

    @Test
    void scoringWhileAiIsDownReturns503() throws Exception {
        when(interviewService.submitAnswer(100L, 201L, "answer", 1L))
                .thenThrow(new OllamaUnavailableException("Ollama is unreachable", null));

        mockMvc.perform(post("/api/interviews/100/questions/201/answer").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"answer\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void completeReturns200WithOverallScore() throws Exception {
        when(interviewService.complete(100L, 1L)).thenReturn(InterviewResponseDto.builder()
                .id(100L).status("COMPLETED").overallScore(64.0).build());

        mockMvc.perform(post("/api/interviews/100/complete").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore").value(64.0));
    }
}
