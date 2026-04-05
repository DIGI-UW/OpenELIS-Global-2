package org.openelisglobal.analyzer.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared response envelope and exception-mapping helpers for analyzer REST
 * controllers.
 *
 * <p>
 * All response bodies use {@link LinkedHashMap} for predictable JSON key
 * ordering.
 */
final class AnalyzerControllerHelper {

    private AnalyzerControllerHelper() {
    }

    /**
     * Wrap successful data in a standard {@code {status, data}} envelope.
     */
    static Map<String, Object> wrapResponse(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    /**
     * Build a standard error envelope: {@code {status: "error", error: message}}.
     */
    static Map<String, Object> wrapError(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "error");
        response.put("error", message);
        return response;
    }

    /**
     * Map a {@link LIMSRuntimeException} to an appropriate HTTP status based on its
     * root cause.
     *
     * <ul>
     * <li>{@link IllegalArgumentException} &rarr; 400 BAD_REQUEST</li>
     * <li>{@link org.hibernate.StaleObjectStateException} &rarr; 409 CONFLICT</li>
     * <li>{@link org.hibernate.exception.ConstraintViolationException} &rarr; 409
     * CONFLICT</li>
     * <li>Everything else &rarr; 500 INTERNAL_SERVER_ERROR</li>
     * </ul>
     */
    static ResponseEntity<Map<String, Object>> mapExceptionToResponse(LIMSRuntimeException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapError(e.getMessage()));
        }
        if (cause instanceof org.hibernate.StaleObjectStateException) {
            Map<String, Object> body = wrapError("Concurrent edit detected. Please reload and try again.");
            body.put("code", "CONCURRENT_EDIT");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(wrapError(e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapError(e.getMessage()));
    }
}
