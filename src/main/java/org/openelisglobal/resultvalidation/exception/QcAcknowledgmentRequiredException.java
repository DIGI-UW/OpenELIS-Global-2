package org.openelisglobal.resultvalidation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by {@code ResultValidationService.persistdata} when a release is
 * attempted on a batch that has unacknowledged QC failures (S-08 FR-04).
 * Spring's {@link ResponseStatus} maps this to HTTP 400 so the frontend can
 * surface the failure in a notification.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QcAcknowledgmentRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QcAcknowledgmentRequiredException(String message) {
        super(message);
    }
}
