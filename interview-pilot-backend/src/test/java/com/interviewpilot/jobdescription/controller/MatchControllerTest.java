package com.interviewpilot.jobdescription.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewpilot.common.enums.Role;
import com.interviewpilot.exception.ResourceNotFoundException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MatchController.class)
@Import({SecurityConfig.class, AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class})
class MatchControllerTest {

    private static final CustomUserDetails OWNER = new CustomUserDetails(User.builder().id(1L).firstName("Asha")
            .lastName("Rao").email("asha@example.com").password("x").role(Role.USER).build());

    @Autowired private MockMvc mockMvc;
    @MockitoBean private JobDescriptionService jobDescriptionService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void listsOwnMatchesWithJobDetails() throws Exception {
        when(jobDescriptionService.findAllMatchesForUser(1L)).thenReturn(List.of(ResumeMatchResponseDto.builder()
                .id(3L).jobTitle("Backend Engineer").companyName("Acme").matchScore(74.0).build()));

        mockMvc.perform(get("/api/matches").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobTitle").value("Backend Engineer"));
    }

    @Test
    void someoneElsesMatchIsNotFound() throws Exception {
        when(jobDescriptionService.findMatchForUser(99L, 1L)).thenThrow(new ResourceNotFoundException("Match not found"));

        mockMvc.perform(get("/api/matches/99").with(user(OWNER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match not found"));
    }

    @Test
    void anonymousGets401() throws Exception {
        mockMvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
    }
}
