package org.openelisglobal.qa.controller.rest;

import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bad input is 400 across the QA REST surface: the services throw
 * {@link IllegalArgumentException} for unknown ids, malformed codes, duplicate
 * rows and delete-while-referenced, and a
 * {@link DataIntegrityViolationException} — a race past those checks straight
 * into a unique constraint — is answered the same way rather than surfacing as
 * a 500.
 *
 * <p>
 * One advice over the QA controller packages instead of the same pair of
 * handlers on each controller. Modelled on
 * {@code MicrobiologyRestExceptionHandler}. A controller that needs a different
 * answer still declares its own {@code @ExceptionHandler}, which wins over this
 * one.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = { "org.openelisglobal.qa.controller",
        "org.openelisglobal.accreditation.controller", "org.openelisglobal.qaevent.qiconfig.controller",
        "org.openelisglobal.qaevent.criticalcallback.controller", "org.openelisglobal.reports.amendment.controller",
        "org.openelisglobal.reports.rejection.controller" })
public class QaRestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        // Map.of refuses a null value, which would turn this 400 into a 500.
        return Map.of("error", e.getMessage() == null ? "Invalid request" : e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraint(DataIntegrityViolationException e) {
        LogEvent.logError(e);
        return Map.of("error", "Invalid or duplicate data");
    }
}
