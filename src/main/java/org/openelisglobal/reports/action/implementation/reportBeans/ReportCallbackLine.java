package org.openelisglobal.reports.action.implementation.reportBeans;

/**
 * The latest critical-value callback logged for a result (FRS FR-A33).
 *
 * @param status          {@code CONFIRMED}, {@code REACHED_NO_READBACK} or
 *                        {@code UNABLE_TO_REACH}
 * @param earlierAttempts how many callbacks were logged before this one
 */
public record ReportCallbackLine(String recipient, String loggedAt, String loggedBy, String status,
        int earlierAttempts) {
}
