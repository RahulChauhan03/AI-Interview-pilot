package com.interviewpilot.exception;

import com.interviewpilot.auth.dto.ErrorResponseDto;
import com.interviewpilot.common.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central exception translation. Every error is returned in the {@link ApiResponse} envelope.
 * Messages sent to the client are safe to show; details of server-side failures are only logged.
 * Spring MVC's own exceptions (unknown endpoint, malformed JSON, missing upload part, oversized
 * upload, wrong HTTP method...) are mapped to their proper status by {@link ResponseEntityExceptionHandler}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred";

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "EMAIL_ALREADY_EXISTS", null);
    }

    @ExceptionHandler({InvalidCredentialsException.class, UnauthorizedException.class})
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleUnauthorized(CustomException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), "UNAUTHORIZED", null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "RESOURCE_NOT_FOUND", null);
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleInvalidResetToken(InvalidResetTokenException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "INVALID_RESET_TOKEN", null);
    }

    @ExceptionHandler(ExpiredResetTokenException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleExpiredResetToken(ExpiredResetTokenException exception) {
        return error(HttpStatus.GONE, exception.getMessage(), "EXPIRED_RESET_TOKEN", null);
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleEmailSending(EmailSendingException exception) {
        // EmailServiceImpl already logs the cause.
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process the password reset request.", "EMAIL_SENDING_ERROR", null);
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleFileTooLarge(FileTooLargeException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage(), "FILE_TOO_LARGE", null);
    }

    @ExceptionHandler(UnsupportedFileException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleUnsupportedFile(UnsupportedFileException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "RESUME_PROCESSING_ERROR", null);
    }

    @ExceptionHandler(ResumeParsingException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleResumeParsing(ResumeParsingException exception) {
        // The exception message can contain parser internals, so it is logged, not returned.
        log.warn("Resume parsing failed: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, "The resume file could not be read", "RESUME_PROCESSING_ERROR", null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleConflict(ConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "CONFLICT", null);
    }

    /** Ollama answered, but with output that failed validation; more specific than the 503 handler below. */
    @ExceptionHandler(InvalidAiResponseException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleInvalidAiResponse(InvalidAiResponseException exception) {
        log.error("AI response rejected: {}", exception.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "The AI service returned an unusable answer. Please try again.",
                "AI_INVALID_RESPONSE", null);
    }

    /** Ollama failures, and the "ollama" circuit breaker rejecting calls while it is open. */
    @ExceptionHandler({OllamaException.class, CallNotPermittedException.class})
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleAiServiceFailure(RuntimeException exception) {
        log.error("AI service call failed", exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "The AI service is temporarily unavailable. Please try again later.",
                "AI_SERVICE_UNAVAILABLE", null);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleStorageFailure(StorageException exception) {
        log.error("File storage failed", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store the uploaded file", "STORAGE_ERROR", null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            validationErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR", validationErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleUnexpected(Exception exception) {
        log.error("Unhandled exception on {}", currentPath(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_ERROR_MESSAGE, "INTERNAL_ERROR", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(apiError(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR", validationErrors));
    }

    /** Renders Spring MVC's built-in exceptions in the project's error format. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String message;
        String error;
        if (status.is5xxServerError()) {
            log.error("Request failed on {}", currentPath(), exception);
            message = GENERIC_ERROR_MESSAGE;
            error = "INTERNAL_ERROR";
        } else if (status == HttpStatus.NOT_FOUND) {
            message = "The requested resource was not found";
            error = "RESOURCE_NOT_FOUND";
        } else if (status == HttpStatus.PAYLOAD_TOO_LARGE) {
            message = "File size exceeds the maximum allowed limit";
            error = "FILE_TOO_LARGE";
        } else {
            // Spring's default detail text is client-safe, e.g. "Required part 'file' is not present."
            message = body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null
                    ? problemDetail.getDetail()
                    : status.getReasonPhrase();
            error = status.name();
        }
        return super.handleExceptionInternal(exception, apiError(status, message, error, null), headers, statusCode, request);
    }

    private ResponseEntity<ApiResponse<ErrorResponseDto>> error(
            HttpStatus status, String message, String error, Map<String, String> validationErrors) {
        return ResponseEntity.status(status).body(apiError(status, message, error, validationErrors));
    }

    private ApiResponse<ErrorResponseDto> apiError(
            HttpStatus status, String message, String error, Map<String, String> validationErrors) {
        ErrorResponseDto details = ErrorResponseDto.builder()
                .error(error)
                .validationErrors(validationErrors)
                .build();
        return ApiResponse.<ErrorResponseDto>builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(details)
                .path(currentPath())
                .build();
    }

    private String currentPath() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest().getRequestURI()
                : null;
    }
}
