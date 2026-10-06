package com.interviewpilot.jobdescription.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewpilot.common.enums.Role;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
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

/** Runs the controller behind the real security configuration and exception handler. */
@WebMvcTest(JobDescriptionController.class)
@Import({SecurityConfig.class, AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class})
class JobDescriptionControllerTest {

    private static final String VALID_BODY =
            "{\"companyName\":\"Acme\",\"jobTitle\":\"Backend Engineer\",\"jobDescription\":\"Java and Spring Boot\"}";
    private static final CustomUserDetails OWNER = new CustomUserDetails(User.builder().id(1L).firstName("Asha")
            .lastName("Rao").email("asha@example.com").password("x").role(Role.USER).build());

    @Autowired private MockMvc mockMvc;
    @MockitoBean private JobDescriptionService jobDescriptionService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void createReturns201ForTheAuthenticatedUser() throws Exception {
        when(jobDescriptionService.create(any(), eq(1L))).thenReturn(JobDescriptionResponseDto.builder()
                .id(10L).userId(1L).companyName("Acme").jobTitle("Backend Engineer").build());

        mockMvc.perform(post("/api/job-descriptions").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/job-descriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("UNAUTHORIZED"));
        verifyNoInteractions(jobDescriptionService);
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/job-descriptions").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"\",\"jobTitle\":\"Engineer\",\"jobDescription\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.validationErrors.companyName").value("Company name is required"))
                .andExpect(jsonPath("$.data.validationErrors.jobDescription").value("Job description is required"));
        verifyNoInteractions(jobDescriptionService);
    }

    @Test
    void listAndGetReturn200() throws Exception {
        when(jobDescriptionService.findAllForUser(1L)).thenReturn(List.of(JobDescriptionResponseDto.builder().id(10L).build()));
        when(jobDescriptionService.findByIdForUser(10L, 1L)).thenReturn(JobDescriptionResponseDto.builder().id(10L).build());

        mockMvc.perform(get("/api/job-descriptions").with(user(OWNER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10));
        mockMvc.perform(get("/api/job-descriptions/10").with(user(OWNER))).andExpect(status().isOk());
    }

    @Test
    void someoneElsesOrMissingJobDescriptionReturns404() throws Exception {
        when(jobDescriptionService.findByIdForUser(77L, 1L)).thenThrow(new ResourceNotFoundException("Job description not found"));

        mockMvc.perform(get("/api/job-descriptions/77").with(user(OWNER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job description not found"));
    }

    @Test
    void updateAndDeleteReturn200() throws Exception {
        when(jobDescriptionService.update(eq(10L), any(), eq(1L))).thenReturn(JobDescriptionResponseDto.builder().id(10L).build());

        mockMvc.perform(put("/api/job-descriptions/10").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/job-descriptions/10").with(user(OWNER))).andExpect(status().isOk());
    }

    @Test
    void deleteInUseReturns409() throws Exception {
        doThrow(new ConflictException("This job description is used by an interview and cannot be deleted"))
                .when(jobDescriptionService).delete(10L, 1L);

        mockMvc.perform(delete("/api/job-descriptions/10").with(user(OWNER)))
                .andExpect(status().isConflict());
    }

    @Test
    void matchReturns201WithScore() throws Exception {
        when(jobDescriptionService.matchResume(10L, 5L, 1L)).thenReturn(ResumeMatchResponseDto.builder()
                .id(1L).resumeId(5L).jobDescriptionId(10L).matchScore(81.0).missingSkills(List.of("Kafka")).build());

        mockMvc.perform(post("/api/job-descriptions/10/matches").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resumeId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.matchScore").value(81.0))
                .andExpect(jsonPath("$.data.missingSkills[0]").value("Kafka"));
    }

    @Test
    void reusedMatchReturns200() throws Exception {
        when(jobDescriptionService.matchResume(10L, 5L, 1L)).thenReturn(ResumeMatchResponseDto.builder()
                .id(1L).matchScore(81.0).reused(true).build());

        mockMvc.perform(post("/api/job-descriptions/10/matches").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resumeId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reused").value(true))
                .andExpect(jsonPath("$.message").value("Previous match result reused"));
    }

    @Test
    void matchWithoutResumeReturns400() throws Exception {
        mockMvc.perform(post("/api/job-descriptions/10/matches").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.validationErrors.resumeId").value("Resume is required"));
    }

    @Test
    void aiUnavailableReturns503AndInvalidAiAnswerReturns502() throws Exception {
        when(jobDescriptionService.matchResume(10L, 5L, 1L))
                .thenThrow(new OllamaUnavailableException("Ollama is unreachable", null))
                .thenThrow(new InvalidAiResponseException("AI match score is outside 0-100"));

        mockMvc.perform(post("/api/job-descriptions/10/matches").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resumeId\":5}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.error").value("AI_SERVICE_UNAVAILABLE"));
        mockMvc.perform(post("/api/job-descriptions/10/matches").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resumeId\":5}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.data.error").value("AI_INVALID_RESPONSE"));
    }
}
