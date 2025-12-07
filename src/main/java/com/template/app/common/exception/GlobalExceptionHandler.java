package com.template.app.common.exception;

import com.template.app.auth.infrastructure.exception.AccountLockedException;
import com.template.app.auth.infrastructure.exception.EmailAlreadyExistsException;
import com.template.app.auth.infrastructure.exception.InvalidCredentialsException;
import com.template.app.auth.infrastructure.exception.InvalidOAuth2TokenException;
import com.template.app.auth.infrastructure.exception.InvalidPasswordException;
import com.template.app.auth.infrastructure.exception.InvalidTokenException;
import com.template.app.auth.infrastructure.exception.UserAlreadyExistsException;
import com.template.app.auth.infrastructure.exception.UserNotFoundException;
import com.template.app.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidOAuth2TokenException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidOAuth2Token(InvalidOAuth2TokenException e) {
        log.error("Invalid OAuth2 token: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<String>> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        log.warn("Email already exists: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException e) {
        log.error("Invalid token error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("INVALID_TOKEN")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException e) {
        log.error("User not found error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("USER_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException e) {
        log.error("User already exists error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("USER_ALREADY_EXISTS")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException e) {
        log.error("Invalid password error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("INVALID_PASSWORD")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException e) {
        log.warn("Account locked: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("ACCOUNT_LOCKED")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
        log.error("Bad credentials error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message("Invalid phone number or password")
                .code("BAD_CREDENTIALS")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException e) {
        log.warn("Invalid credentials: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("INVALID_CREDENTIALS")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException e) {

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("ILLEGAL_STATE")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException e) {
        log.error("Security error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .code("SECURITY_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(
            MissingRequestHeaderException e) {
        log.error("Missing required header: {}", e.getHeaderName());

        ErrorResponse error = ErrorResponse.builder()
                .message(String.format("Required request header '%s' is missing", e.getHeaderName()))
                .code("MISSING_HEADER")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException e) {

        Map<String, String> validationErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse error = ErrorResponse.builder()
                .message("Validation failed")
                .code("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .details(Map.of("errors", validationErrors))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException e) {
        log.error("Validation constraint violation: {}", e.getMessage());

        String violationMessage = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        ErrorResponse error = ErrorResponse.builder()
                .message(violationMessage)
                .code("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unhandled exception", e);

        ErrorResponse error = ErrorResponse.builder()
                .message("An unexpected error occurred")
                .code("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(error));
    }
}
