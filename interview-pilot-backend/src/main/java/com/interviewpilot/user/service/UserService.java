package com.interviewpilot.user.service;

import com.interviewpilot.user.dto.ProfileUpdateRequestDto;
import com.interviewpilot.user.dto.UserResponseDto;
import java.util.List;

public interface UserService {
    UserResponseDto findById(Long userId);
    UserResponseDto updateProfile(Long userId, ProfileUpdateRequestDto request);
    /** For administrators: safe fields only, never the password hash. */
    List<UserResponseDto> findAll();
}
