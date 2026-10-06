package com.interviewpilot.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewpilot.common.enums.Role;
import com.interviewpilot.security.config.SecurityConfig;
import com.interviewpilot.security.handler.AccessDeniedHandlerImpl;
import com.interviewpilot.security.handler.AuthenticationEntryPointImpl;
import com.interviewpilot.security.jwt.JwtService;
import com.interviewpilot.security.service.CustomUserDetails;
import com.interviewpilot.security.service.CustomUserDetailsService;
import com.interviewpilot.user.dto.UserResponseDto;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class})
class UserControllerTest {

    private static final CustomUserDetails ME = new CustomUserDetails(User.builder().id(7L).firstName("Asha")
            .lastName("Rao").email("asha@example.com").password("x").role(Role.USER).build());

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void returnsOwnProfile() throws Exception {
        when(userService.findById(7L)).thenReturn(UserResponseDto.builder().id(7L).firstName("Asha").role("USER").build());

        mockMvc.perform(get("/api/users/me").with(user(ME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Asha"));
    }

    @Test
    void updatesOnlyTheName() throws Exception {
        when(userService.updateProfile(eq(7L), any())).thenReturn(UserResponseDto.builder().id(7L).firstName("Asha").lastName("Iyer").build());

        mockMvc.perform(put("/api/users/me").with(user(ME)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Asha\",\"lastName\":\"Iyer\",\"role\":\"ADMIN\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastName").value("Iyer"));
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mockMvc.perform(put("/api/users/me").with(user(ME)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\" \",\"lastName\":\"Iyer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.validationErrors.firstName").value("First name is required"));
        verifyNoInteractions(userService);
    }

    @Test
    void anonymousGets401() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
}
