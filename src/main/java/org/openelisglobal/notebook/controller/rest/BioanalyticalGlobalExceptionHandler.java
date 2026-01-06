package org.openelisglobal.notebook.controller.rest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for Bioanalytical REST controllers.
 *
 * Provides centralized, consistent error handling and responses across all
 * bioanalytical API endpoints with: - Structured error responses with request
 * tracking IDs - Detailed validation error messages - Appropriate HTTP status
 * codes - Timestamp logging for audit trail
 *
 * Handles exceptions: - Validation errors (MethodArgumentNotValidException) -
 * Type mismatches (MethodArgumentTypeMismatchException) - File upload size
 * limits (MaxUploadSizeExceededException) - Resource not found
 * (NoHandlerFoundException) - General exceptions and runtime errors
 */
@ControllerAdvice
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BioanalyticalGlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotation.
     *
     * Returns 400 Bad Request with list of field validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse("Validation failed", HttpStatus.BAD_REQUEST, request);

        List<Map<String, String>> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("field", fieldError.getField());
                errorMap.put("message", fieldError.getDefaultMessage());
                errorMap.put("rejectedValue",
                        fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : "null");
                fieldErrors.add(errorMap);
            }
        });

        response.put("validationErrors", fieldErrors);
        response.put("errorCount", fieldErrors.size());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles type mismatch errors (e.g., invalid date format, wrong parameter
     * type).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                String.format("Invalid parameter '%s': expected %s but received '%s'", ex.getName(),
                        ex.getRequiredType().getSimpleName(), ex.getValue()),
                HttpStatus.BAD_REQUEST, request);

        response.put("parameter", ex.getName());
        response.put("expectedType", ex.getRequiredType().getSimpleName());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles file upload size limit exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileSizeExceededException(MaxUploadSizeExceededException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse("File size exceeds maximum allowed size",
                HttpStatus.PAYLOAD_TOO_LARGE, request);

        response.put("maxSize", ex.getMaxUploadSize());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * Handles resource not found (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(NoHandlerFoundException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse(
                String.format("Resource not found: %s %s", ex.getHttpMethod(), ex.getRequestURL()),
                HttpStatus.NOT_FOUND, request);

        response.put("path", ex.getRequestURL());
        response.put("method", ex.getHttpMethod());

        return ResponseEntity.notFound().build();
    }

    /**
     * Handles illegal argument exceptions (e.g., invalid enum value, invalid date
     * range).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse("Invalid argument: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST, request);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles illegal state exceptions (e.g., entry already has approved results).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex,
            WebRequest request) {

        Map<String, Object> response = buildErrorResponse("Invalid operation: " + ex.getMessage(), HttpStatus.CONFLICT,
                request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles all other runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {

        Map<String, Object> response = buildErrorResponse("An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR, request);

        // Log the exception for debugging (in production, use proper logging)
        ex.printStackTrace();

        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {

        Map<String, Object> response = buildErrorResponse("An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);

        // Log the exception for debugging (in production, use proper logging)
        ex.printStackTrace();

        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * Builds a standard error response structure.
     *
     * @param message Error message
     * @param status  HTTP status
     * @param request Web request for context
     * @return Structured error response map
     */
    private Map<String, Object> buildErrorResponse(String message, HttpStatus status, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("status", status.value());
        response.put("statusText", status.getReasonPhrase());
        response.put("timestamp", LocalDateTime.now());
        response.put("requestId", generateRequestId());
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return response;
    }

    /**
     * Generates a unique request ID for tracing and auditing.
     */
    private String generateRequestId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
