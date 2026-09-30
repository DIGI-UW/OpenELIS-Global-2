package org.openelisglobal.eqa.valueholder;

/**
 * Participant-result lifecycle: DRAFT → VALIDATED_PARTIAL → SUBMITTED → SCORED,
 * with MISSED_DEADLINE as the timer-expiry terminal.
 */
public enum EQASubmissionStatus {
    DRAFT, VALIDATED_PARTIAL, SUBMITTED, SCORED, MISSED_DEADLINE
}
