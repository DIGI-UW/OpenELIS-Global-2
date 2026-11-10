package org.openelisglobal.spi.report;

/**
 * Thrown by a {@link ReportProvider} when the requested report cannot be
 * produced.
 */
public class ReportException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReportException(String message) {
        super(message);
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
