package com.interviewpilot.common.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Standard envelope returned by every HTTP endpoint. */
@Getter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final LocalDateTime timestamp;
    private final T data;
}
