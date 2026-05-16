package com.booknest.common.exception;

import com.booknest.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("IllegalArgumentException at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.warn("IllegalStateException at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("ResourceNotFoundException at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation error at {}: {}", request.getRequestURI(), message);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        String exClassName = ex.getClass().getName();
        String superClassName = ex.getClass().getSuperclass() != null ? ex.getClass().getSuperclass().getName() : "";
        
        // Handle Security Exceptions without direct dependency
        if (exClassName.equals("org.springframework.security.access.AccessDeniedException")) {
            log.warn("Access Denied at {}: {}", request.getRequestURI(), ex.getMessage());
            return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request.getRequestURI());
        }
        if (exClassName.equals("org.springframework.security.core.AuthenticationException") || 
            superClassName.equals("org.springframework.security.core.AuthenticationException")) {
            log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
            return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request.getRequestURI());
        }

        // Handle Feign Exceptions
        if (exClassName.equals("feign.FeignException") || superClassName.equals("feign.FeignException")) {
            try {
                java.lang.reflect.Method statusMethod = ex.getClass().getMethod("status");
                int status = (int) statusMethod.invoke(ex);
                HttpStatus httpStatus = HttpStatus.resolve(status);
                if (httpStatus == null) httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                if (httpStatus.is4xxClientError()) {
                    log.warn("Feign client error (status {}) at {}: {}", status, request.getRequestURI(), ex.getMessage());
                } else {
                    log.error("Feign server error (status {}) at {}: {}", status, request.getRequestURI(), ex.getMessage());
                }
                return buildErrorResponse(ex, httpStatus, request.getRequestURI());
            } catch (Exception ignored) { }
        }
        
        log.error("Unhandled exception [{}] at {}: {}", exClassName, request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, String path) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(ex.getMessage())
                .path(path)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}
