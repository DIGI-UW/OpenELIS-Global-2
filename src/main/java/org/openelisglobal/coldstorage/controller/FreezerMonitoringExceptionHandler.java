package org.openelisglobal.coldstorage.controller;

import lombok.Data;
import org.openelisglobal.coldstorage.service.exception.FreezerDeviceNotFoundException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Scoped to this package (not a bare @ControllerAdvice) and ordered ahead of
// ControllerSetup's app-wide handler, which deliberately returns no exception
// detail for LIMSRuntimeException/RuntimeException - fine for most of the app,
// but it turned every device-creation validation failure (duplicate name/code,
// bad room, etc.) into an opaque "Internal Server Error" here (issue #3904).
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

    @ExceptionHandler(LIMSRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleStorageValidation(LIMSRuntimeException ex) {
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
