package com.truholdem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.truholdem.domain.exception.GameDomainException;
import com.truholdem.domain.exception.GameStateException;
import com.truholdem.domain.exception.InvalidActionException;
import com.truholdem.domain.exception.PlayerNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        List<FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        logger.warn("Validation failed: {} errors - {}", 
                fieldErrors.size(), 
                fieldErrors.stream().map(FieldValidationError::field).collect(Collectors.joining(", ")));

        ValidationErrorResponse error = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed",
                getPath(request),
                getCorrelationId(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        
        List<FieldValidationError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new FieldValidationError(
                        getPropertyName(violation),
                        violation.getInvalidValue(),
                        violation.getMessage()))
                .collect(Collectors.toList());

        logger.warn("Constraint violation: {} errors", fieldErrors.size());

        ValidationErrorResponse error = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Constraint validation failed",
                getPath(request),
                getCorrelationId(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        logger.warn("Type mismatch: {}", message);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                getPath(request),
                getCorrelationId()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Bad request: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            getPath(request),
            getCorrelationId()
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        logger.warn("Invalid state: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            getPath(request),
            getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoSuchElementException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            getPath(request),
            getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            getPath(request),
            getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(GameDomainException.class)
    public ResponseEntity<DomainErrorResponse> handleGameDomainException(
            GameDomainException ex, WebRequest request) {
        
        HttpStatus status = switch (ex) {
            case InvalidActionException e -> HttpStatus.BAD_REQUEST;
            case GameStateException e -> HttpStatus.CONFLICT;
            case PlayerNotFoundException e -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        logger.warn("Domain exception [{}]: {} - {}", 
                ex.getErrorCode(), 
                ex.getClass().getSimpleName(), 
                ex.getMessage());

        DomainErrorResponse error = new DomainErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                getPath(request),
                getCorrelationId(),
                ex.getErrorCode(),
                ex.getContext()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(GameException.class)
    public ResponseEntity<ErrorResponse> handleGameException(
            GameException ex, WebRequest request) {
        logger.error("Game error: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            getPath(request),
            getCorrelationId()
        );
        
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error [correlationId={}]", getCorrelationId(), ex);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            getPath(request),
            getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : "unknown";
    }

    private String getPropertyName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot + 1) : path;
    }

    
    public static class ErrorResponse {
        private final LocalDateTime timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;
        private final String correlationId;

        public ErrorResponse(int status, String error, String message, String path, String correlationId) {
            this.timestamp = LocalDateTime.now();
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.correlationId = correlationId;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public String getCorrelationId() { return correlationId; }
    }

    
    public static class ValidationErrorResponse extends ErrorResponse {
        private final List<FieldValidationError> fieldErrors;

        public ValidationErrorResponse(int status, String error, String message, 
                String path, String correlationId, List<FieldValidationError> fieldErrors) {
            super(status, error, message, path, correlationId);
            this.fieldErrors = fieldErrors;
        }

        public List<FieldValidationError> getFieldErrors() { return fieldErrors; }
    }

    
    public record FieldValidationError(
            String field,
            Object rejectedValue,
            String message
    ) {}

    
    public static class DomainErrorResponse extends ErrorResponse {
        private final String errorCode;
        private final Map<String, Object> context;

        public DomainErrorResponse(int status, String error, String message,
                String path, String correlationId, String errorCode, 
                Map<String, Object> context) {
            super(status, error, message, path, correlationId);
            this.errorCode = errorCode;
            this.context = context != null ? context : Collections.emptyMap();
        }

        public String getErrorCode() { return errorCode; }
        public Map<String, Object> getContext() { return context; }
    }
}
