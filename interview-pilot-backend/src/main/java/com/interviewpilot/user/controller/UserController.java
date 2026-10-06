package com.interviewpilot.user.controller;

import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.security.service.CustomUserDetails;
import com.interviewpilot.user.dto.ProfileUpdateRequestDto;
import com.interviewpilot.user.dto.UserResponseDto;
import com.interviewpilot.user.service.UserService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own profile. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponseDto> me(@AuthenticationPrincipal CustomUserDetails user) {
        return response("Profile retrieved", userService.findById(user.getUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponseDto> updateMe(@Valid @RequestBody ProfileUpdateRequestDto request,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return response("Profile updated", userService.updateProfile(user.getUserId(), request));
    }

    private <T> ApiResponse<T> response(String message, T data) {
        return ApiResponse.<T>builder().status(HttpStatus.OK.value()).message(message).timestamp(LocalDateTime.now()).data(data).build();
    }
}
