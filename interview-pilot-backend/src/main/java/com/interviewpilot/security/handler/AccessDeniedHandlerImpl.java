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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<ErrorResponseDto> body = ApiResponse.<ErrorResponseDto>builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("You do not have permission to access this resource")
                .timestamp(LocalDateTime.now())
                .data(ErrorResponseDto.builder().error("ACCESS_DENIED").build())
                .path(request.getRequestURI())
                .build();
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
