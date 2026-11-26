package org.openelisglobal.program.controller;

import java.util.Map;
import org.openelisglobal.program.controller.cytology.CytologyController;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistryController;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A case save the service refuses (e.g. a technician not allowed on the unit)
 * is a 400, not a 500. Ranked first: the global ControllerSetup
 * (HIGHEST_PRECEDENCE + 1) answers every RuntimeException with a 500 and would
 * otherwise win.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = { PathologyController.class, CytologyController.class,
        ImmunohistochemistryController.class })
public class ProgramCaseExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> refused(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
