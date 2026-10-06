package com.interviewpilot.admin.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewpilot.admin.dto.AdminStatsDto;
import com.interviewpilot.admin.service.AdminService;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Admin endpoints behind the real security configuration. */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class})
class AdminControllerTest {

    private static final CustomUserDetails ADMIN = principal(1L, Role.ADMIN);
    private static final CustomUserDetails CANDIDATE = principal(2L, Role.USER);

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminService adminService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/stats", "/api/admin/users", "/api/admin/activity", "/api/admin/system"})
    void regularUserGets403(String path) throws Exception {
        mockMvc.perform(get(path).with(user(CANDIDATE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.error").value("ACCESS_DENIED"));
        verifyNoInteractions(adminService, userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/stats", "/api/admin/users", "/api/admin/activity", "/api/admin/system"})
    void anonymousGets401(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminGetsStats() throws Exception {
        when(adminService.stats()).thenReturn(AdminStatsDto.builder().users(3).resumes(5).interviews(2).build());

        mockMvc.perform(get("/api/admin/stats").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").value(3))
                .andExpect(jsonPath("$.data.resumes").value(5));
    }

    @Test
    void adminUserListNeverContainsPasswords() throws Exception {
        when(userService.findAll()).thenReturn(List.of(UserResponseDto.builder().id(1L).email("a@example.com").role("ADMIN").build()));

        mockMvc.perform(get("/api/admin/users").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("a@example.com"))
                .andExpect(jsonPath("$.data[0]", not(hasKey("password"))));
    }

    @Test
    void activityLimitIsCapped() throws Exception {
        when(adminService.recentActivity(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/activity?limit=100000").with(user(ADMIN))).andExpect(status().isOk());

        verify(adminService).recentActivity(200);
    }

    private static CustomUserDetails principal(Long id, Role role) {
        return new CustomUserDetails(User.builder().id(id).firstName("Test").lastName("User")
                .email(role.name().toLowerCase() + "@example.com").password("x").role(role).build());
    }
}
