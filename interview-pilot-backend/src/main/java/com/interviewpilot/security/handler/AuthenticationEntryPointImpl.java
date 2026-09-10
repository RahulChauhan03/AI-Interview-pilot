package com.interviewpilot.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.auth.dto.ErrorResponseDto;
import com.interviewpilot.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required", "UNAUTHORIZED");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String error)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<ErrorResponseDto> body = ApiResponse.<ErrorResponseDto>builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(ErrorResponseDto.builder().error(error).build())
                .build();
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
