package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.ConsultationStatus;

/**
 * An answer or a withdrawal was asked of a second opinion that is no longer
 * awaited: another user answered or withdrew it first, so the page that sent it
 * is out of date and nothing is written.
 */
public class PathologyConsultationConflictException extends RuntimeException {

    private final ConsultationStatus storedStatus;

    public PathologyConsultationConflictException(Integer consultationId, ConsultationStatus storedStatus) {
        super("second opinion " + consultationId + " is " + storedStatus + ", no longer "
                + ConsultationStatus.REQUESTED);
        this.storedStatus = storedStatus;
    }

    /** The status the second opinion actually holds. */
    public ConsultationStatus getStoredStatus() {
        return storedStatus;
    }
}
