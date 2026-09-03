package org.openelisglobal.resultvalidation.util;

import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;

/**
 * Pure rules behind the Validation queue's "Check before release" signals
 * (OGC-1027, Validation v4 slice V1).
 *
 * <p>
 * Kept free of Spring and persistence so every rule is unit-testable in
 * isolation; {@link ResultsValidationUtility} loads the inputs and calls these.
 * The fail-safe posture of FR-B1 applies throughout: an indeterminate input is
 * read as risk present, never as clearance.
 */
public final class ValidationSignals {

    /** QC evaluated for the analysis and every check passed. */
    public static final String QC_PASS = "PASS";
    /** QC evaluated for the analysis and at least one check failed. */
    public static final String QC_FAIL = "FAIL";
    /** No QC evaluation exists — never to be read as "QC passed". */
    public static final String QC_UNKNOWN = "UNKNOWN";

    /**
     * {@code NcEvent.status} values meaning the non-conformity was worked to
     * completion.
     */
    private static final Set<String> TERMINAL_NCE_STATUSES = Set.of("Closed", "Completed");

    private ValidationSignals() {
    }

    /**
     * A result changed after its first save. {@code ResultUtil} stamps revision "1"
     * on the first save of a result and increments it on every later change, so a
     * revision above 1 means the value the validator sees is not the one first
     * entered.
     */
    public static boolean isModified(String revision) {
        if (GenericValidator.isBlankOrNull(revision)) {
            return false;
        }
        try {
            return Integer.parseInt(revision.trim()) > 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Mirrors {@code TestAlertEvaluationServiceImpl#isCriticalValue} (OGC-1022): a
     * numeric value outside an authored critical bound of the result's own limit. A
     * bound is authored only when finite — the infinities are the "not authored"
     * sentinels — so a test without critical limits never fires. Must be evaluated
     * on the raw {@link ResultLimit}: the validation beans later collapse
     * unauthored bounds to 0, which would make every positive value read as
     * critical-high.
     */
    public static boolean isCritical(ResultLimit limit, Result result) {
        if (limit == null || result == null || !"N".equals(result.getResultType())) {
            return false;
        }
        String value = result.getValue();
        if (GenericValidator.isBlankOrNull(value)) {
            return false;
        }
        try {
            double numeric = Double.parseDouble(value.trim());
            boolean low = Double.isFinite(limit.getLowCritical()) && numeric < limit.getLowCritical();
            boolean high = Double.isFinite(limit.getHighCritical()) && numeric > limit.getHighCritical();
            return low || high;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * A non-conformity still carries risk until it reaches a terminal status. A
     * missing status is indeterminate and is therefore open (fail-safe).
     */
    public static boolean isNceOpen(String status) {
        if (GenericValidator.isBlankOrNull(status)) {
            return true;
        }
        return !TERMINAL_NCE_STATUSES.contains(status.trim());
    }

    /**
     * A critical-value alert raised on the analysis (OGC-1022 posts one per
     * critical result) that nobody has acknowledged yet. Acknowledging or resolving
     * it on the Alerts dashboard clears the signal.
     */
    public static boolean hasOpenCriticalAlert(List<Alert> alerts) {
        if (alerts == null) {
            return false;
        }
        return alerts.stream().anyMatch(alert -> alert != null && alert.getAlertType() == AlertType.CRITICAL_RESULT
                && alert.getStatus() == AlertStatus.OPEN);
    }
}
