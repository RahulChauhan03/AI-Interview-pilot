package com.interviewpilot.auth.controller;

import com.interviewpilot.auth.dto.LoginRequestDto;
import com.interviewpilot.auth.dto.LoginResponseDto;
import com.interviewpilot.auth.dto.RegisterRequestDto;
import com.interviewpilot.auth.dto.RegisterResponseDto;
import com.interviewpilot.auth.dto.ForgotPasswordRequestDto;
import com.interviewpilot.auth.dto.ResetPasswordRequestDto;
import com.interviewpilot.auth.service.AuthService;
import com.interviewpilot.common.response.ApiResponse;
import com.interviewpilot.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        RegisterResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(HttpStatus.CREATED, "Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(success(HttpStatus.OK, "Login successful", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(success(HttpStatus.OK,
                "If an account exists for this email, a password reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(success(HttpStatus.OK, "Password has been reset successfully.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> currentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        RegisterResponseDto response = RegisterResponseDto.builder()
                .userId(currentUser.getUserId())
                .firstName(currentUser.getFirstName())
                .lastName(currentUser.getLastName())
                .email(currentUser.getEmail())
                .role(currentUser.getRole())
                .build();
        return ResponseEntity.ok(success(HttpStatus.OK, "Current user retrieved", response));
    }

    private <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }
}
