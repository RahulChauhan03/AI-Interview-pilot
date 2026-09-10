package com.interviewpilot.exception;

import com.interviewpilot.auth.dto.ErrorResponseDto;
import com.interviewpilot.common.response.ApiResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Central extension point for application-wide exception translation. */
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process the password reset request.", "EMAIL_SENDING_ERROR", null);
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleFileTooLarge(FileTooLargeException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage(), "FILE_TOO_LARGE", null);
    }

    @ExceptionHandler({UnsupportedFileException.class, ResumeParsingException.class, OllamaException.class, StorageException.class})
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleResumeError(CustomException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "RESUME_PROCESSING_ERROR", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR", validationErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponseDto>> handleUnexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "INTERNAL_ERROR", null);
    }

    private ResponseEntity<ApiResponse<ErrorResponseDto>> error(
            HttpStatus status, String message, String error, Map<String, String> validationErrors) {
        ErrorResponseDto details = ErrorResponseDto.builder()
                .error(error)
                .validationErrors(validationErrors)
                .build();
        ApiResponse<ErrorResponseDto> response = ApiResponse.<ErrorResponseDto>builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(details)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
