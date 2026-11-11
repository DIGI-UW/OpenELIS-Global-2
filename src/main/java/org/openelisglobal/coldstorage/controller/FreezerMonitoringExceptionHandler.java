package org.openelisglobal.coldstorage.controller;

import lombok.Data;
import org.openelisglobal.coldstorage.service.exception.FreezerDeviceNotFoundException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Scoped to this package (not a bare @ControllerAdvice) and strictly ahead of
// ControllerSetup, which deliberately returns no exception detail for
// LIMSRuntimeException/RuntimeException - fine for most of the app, but it turned
// every device-creation validation failure (duplicate name/code, bad room, etc.)
// into an opaque "Internal Server Error" here (issue #3904). ControllerSetup sits
// at HIGHEST_PRECEDENCE + 1 for this; matching orders would leave the winner to
// bean registration order.
@ControllerAdvice(basePackages = "org.openelisglobal.coldstorage.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FreezerMonitoringExceptionHandler {

    @ExceptionHandler(FreezerDeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDeviceNotFound(FreezerDeviceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("device_not_found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("bad_request", ex.getMessage()));
    }

    /**
     * A cause on the exception means a DAO wrapped an infrastructure fault, so it
     * answers 500 rather than blaming the request with a 409.
     */
    @ExceptionHandler(LIMSRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleStorageValidation(LIMSRuntimeException ex) {
        if (ex.getCause() != null) {
            LogEvent.logError(ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("internal_error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("storage_validation_error",
                ex.getMessage() != null ? ex.getMessage() : "Unable to save device"));
    }

    @Data
    public static class ErrorResponse {
        private final String code;
        private final String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
