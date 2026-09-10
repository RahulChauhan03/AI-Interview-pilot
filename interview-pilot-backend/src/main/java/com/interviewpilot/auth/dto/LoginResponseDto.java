package com.interviewpilot.auth.dto;

import com.interviewpilot.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private Role role;
    private long expiresIn;
}
