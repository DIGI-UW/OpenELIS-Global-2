package org.openelisglobal.program.controller.pathology;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * The answers every pathology case endpoint gives the same way: a refusal the
 * screen translates from its message id, and a case id nothing carries as not
 * found. They sit on a shared base rather than a controller advice so they
 * reach only these controllers.
 */
abstract class PathologyRestController extends BaseRestController {

    /** A refusal the screen translates from its message id. */
    @ExceptionHandler(LocalizedValidationException.class)
    public ResponseEntity<Map<String, Object>> refused(LocalizedValidationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getMessage());
        body.put("messageKey", e.getErrorCode());
        body.put("messageArgs", e.getParams() == null ? Map.of() : new HashMap<>(e.getParams()));
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * A case id nothing carries is not found, not a server error. Any other missing
     * row (a user id posted with a save, for one) says nothing about the case, so
     * it stays the server error the application answers for any unexpected failure;
     * a handler declared here is the only one Spring tries, so it is answered here
     * rather than rethrown.
     */
    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ObjectNotFoundException e) {
        if (PathologySample.class.getName().equals(e.getEntityName())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No pathology case with id " + e.getIdentifier()));
        }
        LogEvent.logError(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
    }
}
