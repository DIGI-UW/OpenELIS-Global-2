package org.openelisglobal.microbiology.controller.rest;

import java.util.Map;
import org.openelisglobal.microbiology.service.MicroCaseLockedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.openelisglobal.microbiology.controller.rest")
public class MicrobiologyRestExceptionHandler {

    @ExceptionHandler(MicroCaseLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLockedCase(MicroCaseLockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_CASE_LOCKED", "message", exception.getMessage()));
    }
}
