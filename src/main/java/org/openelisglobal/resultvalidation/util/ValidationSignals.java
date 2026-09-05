package org.openelisglobal.resultvalidation.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

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

    /**
     * Who entered the result (OGC-1028 review summary). Mirrors Results Entry's
     * technician resolution in {@code ResultsLoadUtility}: the bench signature is
     * the last non-supervisor {@link ResultSignature}; supervisor signatures are
     * legacy and ignored. Blank when no bench signature exists.
     */
    public static String enteredBy(List<ResultSignature> signatures) {
        String name = "";
        if (signatures == null) {
            return name;
        }
        for (ResultSignature signature : signatures) {
            if (signature != null && !signature.getIsSupervisor()
                    && !GenericValidator.isBlankOrNull(signature.getNonUserName())) {
                name = signature.getNonUserName();
            }
        }
        return name;
    }

    /**
     * The Clear lane rule (OGC-1029, FR-B1), evaluated server-side on the row the
     * queue itself served so a bulk release never trusts the client's list: in
     * range with a known reference range, QC evaluated and passed, no open
     * non-conformity, not modified after first save, not critical, not
     * nonconforming, no critical-value acknowledgment pending. Fail-safe: any
     * missing or indeterminate input (no range, QC unknown) is not clear.
     */
    public static boolean isClear(AnalysisItem row) {
        if (row == null) {
            return false;
        }
        boolean rangeKnown = !GenericValidator.isBlankOrNull(row.getNormalRange());
        return rangeKnown && row.isNormal() && QC_PASS.equals(row.getQcStatus()) && !row.isNceOpen()
                && !row.isModified() && !row.isCritical() && !row.isNonconforming() && !row.isAckPending();
    }

    /**
     * A multi-component analysis is clear only when every one of its rows is
     * (FR-B1); an analysis with no rows at all is never clear.
     */
    public static boolean allClear(Collection<AnalysisItem> rowsOfOneAnalysis) {
        return rowsOfOneAnalysis != null && !rowsOfOneAnalysis.isEmpty()
                && rowsOfOneAnalysis.stream().allMatch(ValidationSignals::isClear);
    }

    /**
     * The stale-page guard (OGC-1030, FR-J1): the row round-trips the analysis's
     * {@code lastupdated} (epoch millis) as it was when the queue was served; any
     * later save by another validator moves that timestamp, so a mismatch means the
     * page is stale and the action must not proceed. A row served without a token
     * (legacy client) is not checked; a token that cannot be read is treated as
     * stale — the fail-safe direction, since a reload costs nothing and a silent
     * overwrite is exactly what the guard exists to stop.
     */
    public static boolean isStale(String clientToken, java.sql.Timestamp currentLastupdated) {
        if (GenericValidator.isBlankOrNull(clientToken) || currentLastupdated == null) {
            return false;
        }
        try {
            return Long.parseLong(clientToken.trim()) != currentLastupdated.getTime();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * The analysis revision after a validator modifies its result (OGC-1028,
     * FR-D4). Results Entry stamps "1" on first save and increments on later saves
     * ({@code ResultUtil}); a validation-side change must likewise read as
     * {@link #isModified(String) modified}, so the outcome is never below 2 even
     * for a legacy analysis whose revision is blank or 0.
     */
    public static String nextRevision(String current) {
        int revision = 0;
        if (!GenericValidator.isBlankOrNull(current)) {
            try {
                revision = Integer.parseInt(current.trim());
            } catch (NumberFormatException e) {
                revision = 0;
            }
        }
        return String.valueOf(Math.max(revision, 1) + 1);
    }

    /**
     * The result component a queue row belongs to, or {@code null} for a
     * single-component (legacy) test — lets the review panel list a multi-component
     * test's rows in {@code display_order} (FR-C4).
     */
    public static TestResultComponent componentOf(List<TestResultComponent> components, String componentId) {
        if (components == null || GenericValidator.isBlankOrNull(componentId)) {
            return null;
        }
        return components.stream().filter(component -> component != null && componentId.equals(component.getId()))
                .findFirst().orElse(null);
    }
}
