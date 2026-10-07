package com.interviewpilot.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewpilot.application.document.DownloadFile;
import com.interviewpilot.application.dto.ApplicationDto;
import com.interviewpilot.application.service.ApplicationDocumentService;
import com.interviewpilot.application.service.ApplicationService;
import com.interviewpilot.application.service.SkillGapService;
import com.interviewpilot.common.enums.Role;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.security.config.SecurityConfig;
import com.interviewpilot.security.handler.AccessDeniedHandlerImpl;
import com.interviewpilot.security.handler.AuthenticationEntryPointImpl;
import com.interviewpilot.security.jwt.JwtService;
import com.interviewpilot.security.service.CustomUserDetails;
import com.interviewpilot.security.service.CustomUserDetailsService;
import com.interviewpilot.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Runs the controller behind the real security configuration and exception handler. */
@WebMvcTest(ApplicationController.class)
@Import({SecurityConfig.class, AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class})
class ApplicationControllerTest {

    private static final CustomUserDetails OWNER = new CustomUserDetails(User.builder().id(1L).firstName("Asha")
            .lastName("Rao").email("asha@example.com").password("x").role(Role.USER).build());
    private static final CustomUserDetails OTHER_USER = new CustomUserDetails(User.builder().id(2L).firstName("Ravi")
            .lastName("K").email("ravi@example.com").password("x").role(Role.USER).build());

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ApplicationService applicationService;
    @MockitoBean private ApplicationDocumentService documentService;
    @MockitoBean private SkillGapService skillGapService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void resumePdfIsSentAsAnAttachmentWithItsFileName() throws Exception {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        when(documentService.tailoredResumePdf(7L, 1L)).thenReturn(
                new DownloadFile("Asha_Rao_Acme_Fintech_Backend_Engineer_Resume.pdf", "application/pdf", pdf));

        mockMvc.perform(get("/api/applications/7/tailored-resume/pdf").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"Asha_Rao_Acme_Fintech_Backend_Engineer_Resume.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void packageIsSentAsAZip() throws Exception {
        when(documentService.applicationPackage(7L, 1L)).thenReturn(new DownloadFile("Asha_Rao_Acme_Application.zip", "application/zip", new byte[] {80, 75}));

        mockMvc.perform(get("/api/applications/7/package").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Asha_Rao_Acme_Application.zip\""))
                .andExpect(content().contentType("application/zip"));
    }

    @Test
    void downloadsOfAnotherUsersApplicationReturn404() throws Exception {
        when(documentService.coverLetterPdf(7L, 2L)).thenThrow(new ResourceNotFoundException("Application not found"));
        when(documentService.applicationPackage(7L, 2L)).thenThrow(new ResourceNotFoundException("Application not found"));

        mockMvc.perform(get("/api/applications/7/cover-letter/pdf").with(user(OTHER_USER)))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.message").value("Application not found"));
        mockMvc.perform(get("/api/applications/7/package").with(user(OTHER_USER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedDownloadsReturn401() throws Exception {
        mockMvc.perform(get("/api/applications/7/tailored-resume/pdf")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/applications/7/package")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/skill-gaps")).andExpect(status().isUnauthorized());
        verifyNoInteractions(documentService, applicationService, skillGapService);
    }

    @Test
    void createApplicationWorksWithoutABody() throws Exception {
        when(applicationService.createForJob(10L, null, 1L)).thenReturn(ApplicationDto.builder().id(7L).status("SAVED").build());

        mockMvc.perform(post("/api/job-descriptions/10/application").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.status").value("SAVED"));
    }

    @Test
    void unknownStatusIsRejected() throws Exception {
        mockMvc.perform(patch("/api/applications/7/status").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(applicationService);
    }

    @Test
    void statusIsUpdatedForTheAuthenticatedUser() throws Exception {
        when(applicationService.updateStatus(7L, "APPLIED", 1L)).thenReturn(ApplicationDto.builder().id(7L).status("APPLIED").build());

        mockMvc.perform(patch("/api/applications/7/status").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    void emptyCoverLetterIsRejected() throws Exception {
        mockMvc.perform(put("/api/applications/7/cover-letter").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paragraphs\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/applications/7/cover-letter").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paragraphs\":[\" \"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void aiFailuresMapToGatewayErrors() throws Exception {
        when(documentService.generateTailoredResume(eq(7L), isNull(), eq(1L))).thenThrow(new InvalidAiResponseException("bad"));
        when(documentService.generateCoverLetter(eq(7L), any(), anyLong())).thenThrow(new OllamaUnavailableException("down", null));

        mockMvc.perform(post("/api/applications/7/tailored-resume").with(user(OWNER))).andExpect(status().isBadGateway());
        mockMvc.perform(post("/api/applications/7/cover-letter").with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resumeId\":5}"))
                .andExpect(status().isServiceUnavailable());
        verify(documentService).generateCoverLetter(7L, 5L, 1L);
    }

    @Test
    void deleteApplicationChecksOwnership() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Application not found")).when(applicationService).delete(7L, 2L);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/applications/7").with(user(OTHER_USER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/applications/7").with(user(OWNER)))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/applications/7"))
                .andExpect(status().isUnauthorized());
        verify(applicationService).delete(7L, 1L);
    }
}
