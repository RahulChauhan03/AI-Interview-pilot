package com.interviewpilot.auth.service;

import com.interviewpilot.auth.dto.LoginRequestDto;
import com.interviewpilot.auth.dto.LoginResponseDto;
import com.interviewpilot.auth.dto.RegisterRequestDto;
import com.interviewpilot.auth.dto.RegisterResponseDto;
import com.interviewpilot.auth.dto.ForgotPasswordRequestDto;
import com.interviewpilot.auth.dto.ResetPasswordRequestDto;

public interface AuthService {

    RegisterResponseDto register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    void forgotPassword(ForgotPasswordRequestDto request);

    void resetPassword(ResetPasswordRequestDto request);
}
