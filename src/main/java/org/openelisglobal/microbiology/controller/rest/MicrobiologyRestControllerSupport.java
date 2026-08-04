package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroLotSelectionRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseLockedException;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.service.MicroReferenceConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Shared authenticated-actor lookup for microbiology write endpoints. */
abstract class MicrobiologyRestControllerSupport extends BaseRestController {

    @ExceptionHandler(MicroCaseLockedException.class)
    protected ResponseEntity<Map<String, Object>> handleLockedCase(MicroCaseLockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_CASE_LOCKED", "message", exception.getMessage()));
    }

    @ExceptionHandler(MicroReferenceConflictException.class)
    protected ResponseEntity<Map<String, Object>> handleReferenceConflict(MicroReferenceConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", HttpStatus.CONFLICT.value(), "error",
                "MICROBIOLOGY_REFERENCE_CONFLICT", "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Map<String, Object>> handleInvalidReferenceRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", HttpStatus.BAD_REQUEST.value(),
                "error", "MICROBIOLOGY_REFERENCE_INVALID", "message", exception.getMessage()));
    }

    protected String authenticatedUserId(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null || userId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated system user is required");
        }
        return userId;
    }

    protected List<MicroLotSelection> lotSelections(List<MicroLotSelectionRequestForm> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(request -> new MicroLotSelection(request.analysisId, request.testReagentLinkId, request.lotId))
                .toList();
    }
}
