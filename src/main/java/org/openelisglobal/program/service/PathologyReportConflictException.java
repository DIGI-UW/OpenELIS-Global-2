package org.openelisglobal.program.service;

/**
 * A report write that met a row another write had changed or numbered first:
 * the report is already withdrawn, or the version number was taken.
 */
public class PathologyReportConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PathologyReportConflictException(String message) {
        super(message);
    }

    public PathologyReportConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
