package com.interviewpilot.admin.controller;

import com.interviewpilot.admin.dto.ActivityDto;
import com.interviewpilot.admin.dto.AdminStatsDto;
import com.interviewpilot.admin.dto.ComponentStatusDto;
import com.interviewpilot.admin.service.AdminService;
import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.user.dto.UserResponseDto;
import com.interviewpilot.user.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only, read-only endpoints; "/api/admin/**" requires ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final int MAX_ACTIVITY = 200;

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping("/stats")
    public ApiResponse<AdminStatsDto> stats() {
        return response("Statistics retrieved", adminService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponseDto>> users() {
        return response("Users retrieved", userService.findAll());
    }

    @GetMapping("/activity")
    public ApiResponse<List<ActivityDto>> activity(@RequestParam(defaultValue = "50") int limit) {
        return response("Activity retrieved", adminService.recentActivity(Math.max(1, Math.min(limit, MAX_ACTIVITY))));
    }

    @GetMapping("/system")
    public ApiResponse<List<ComponentStatusDto>> system() {
        return response("System status retrieved", adminService.systemStatus());
    }

    private <T> ApiResponse<T> response(String message, T data) {
        return ApiResponse.<T>builder().status(HttpStatus.OK.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
