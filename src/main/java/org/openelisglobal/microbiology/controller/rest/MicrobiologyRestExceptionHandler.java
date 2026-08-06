package org.openelisglobal.microbiology.controller.rest;

import java.util.Map;
import org.openelisglobal.inventory.service.InventoryLotUnavailableException;
import org.openelisglobal.microbiology.service.MicroAmendmentConflictException;
import org.openelisglobal.microbiology.service.MicroAstConflictException;
import org.openelisglobal.microbiology.service.MicroCaseLockedException;
import org.openelisglobal.microbiology.service.MicroCaseWorkflowConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.openelisglobal.microbiology.controller.rest")
public class MicrobiologyRestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("status", HttpStatus.BAD_REQUEST.value(), "error",
                "MICROBIOLOGY_VALIDATION_ERROR", "message", exception.getMessage()));
    }

    @ExceptionHandler(MicroAmendmentConflictException.class)
    public ResponseEntity<Map<String, Object>> handleAmendmentConflict(MicroAmendmentConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_AMENDMENT_CONFLICT", "message", exception.getMessage()));
    }

    @ExceptionHandler(MicroAstConflictException.class)
    public ResponseEntity<Map<String, Object>> handleAstConflict(MicroAstConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_AST_CONFLICT", "message", exception.getMessage()));
    }

    @ExceptionHandler(MicroCaseLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLockedCase(MicroCaseLockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_CASE_LOCKED", "message", exception.getMessage()));
    }

    @ExceptionHandler(MicroCaseWorkflowConflictException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowConflict(MicroCaseWorkflowConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_WORKFLOW_CONFLICT", "message", exception.getMessage()));
    }

    @ExceptionHandler(InventoryLotUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailableLot(InventoryLotUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_LOT_CONFLICT", "message", exception.getCode(), "lotNumber", exception.getLotNumber()));
    }
}
