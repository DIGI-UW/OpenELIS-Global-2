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
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;

/**
 * Pure rules behind the Validation queue's "Check before release" signals
 * (OGC-1027, Validation v4 slice V1).
 *
 * <p>
 * Kept free of Spring and persistence so every rule is unit-testable in
 * isolation; {@link ResultsValidationUtility} loads the inputs and calls these.
 * The fail-safe posture of FR-B1 applies to every input a row genuinely has: an
 * indeterminate input is read as risk present, never as clearance. An input the
 * row does not have at all, such as a quality-control evaluation on a patient
 * sample, is neither (OGC-1226).
 */
public final class ValidationSignals {

    /** QC evaluated for the analysis and every check passed. */
    public static final String QC_PASS = "PASS";
    /** QC evaluated for the analysis and at least one check failed. */
    public static final String QC_FAIL = "FAIL";
    /**
     * No QC evaluation exists. Never read as "QC passed", and since OGC-1226 not
     * read as a risk either: it is the absence of a fact about the row.
     */
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

    /** The four-tier result flag, in order of precedence (OGC-1022 FR-L1). */
    public static final String FLAG_INVALID = "INVALID";
    public static final String FLAG_CRITICAL = "CRITICAL";
    public static final String FLAG_ABNORMAL = "ABNORMAL";
    public static final String FLAG_NORMAL = "NORMAL";

    /**
     * The single critical rule (OGC-1022, OGC-1121): a numeric value outside an
     * authored critical bound of the result's own limit. A bound is authored only
     * when finite — the infinities are the "not authored" sentinels — so a test
     * without critical limits never fires. Must be evaluated on the raw
     * {@link ResultLimit}, never on a bean that has collapsed unauthored bounds.
     */
    public static boolean isCritical(ResultLimit limit, double numeric) {
        if (limit == null) {
            return false;
        }
        boolean low = Double.isFinite(limit.getLowCritical()) && numeric < limit.getLowCritical();
        boolean high = Double.isFinite(limit.getHighCritical()) && numeric > limit.getHighCritical();
        return low || high;
    }

    public static boolean isCritical(ResultLimit limit, Result result) {
        if (limit == null || result == null || !"N".equals(result.getResultType())) {
            return false;
        }
        String value = result.getValue();
        if (GenericValidator.isBlankOrNull(value)) {
            return false;
        }
        try {
            return isCritical(limit, Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * The flag a numeric result carries against the patient-conditional limit:
     * INVALID outside the valid range, CRITICAL outside an authored critical bound,
     * ABNORMAL outside the reference range, NORMAL inside it. Null when there is
     * nothing to judge: no value, a non-numeric type, or the selector's synthetic
     * empty limit (a null id) — no authored range matched this patient, so there is
     * no basis to call anything "normal". Shared by Results Entry and Validation
     * (OGC-1121) so the two screens can never disagree.
     */
    public static String resultFlag(ResultLimit limit, String resultType, String value) {
        if (limit == null || GenericValidator.isBlankOrNull(limit.getId()) || !"N".equals(resultType)
                || GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        try {
            double numeric = Double.parseDouble(value.trim());
            if (numeric < limit.getLowValid() || numeric > limit.getHighValid()) {
                return FLAG_INVALID;
            }
            if (isCritical(limit, numeric)) {
                return FLAG_CRITICAL;
            }
            if (numeric < limit.getLowNormal() || numeric > limit.getHighNormal()) {
                return FLAG_ABNORMAL;
            }
            return FLAG_NORMAL;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** An authored (finite) bound as itself, an unauthored one as null. */
    public static Double authoredBound(double bound) {
        return Double.isFinite(bound) ? Double.valueOf(bound) : null;
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
     * The clearance rule, stated once for every consumer: the queue's lanes, the
     * bulk release and automated validation at result entry (OGC-1029 FR-B1 as
     * superseded by OGC-1226 FR-1 to FR-3). A row is clear when its reference range
     * is known and the value sits inside it, nothing has been raised against it (no
     * open non-conformity, not modified after first save, not critical, not
     * nonconforming, no critical-value acknowledgment pending) and it carries no
     * recorded quality-control failure. The fail-safe posture holds for every input
     * the row genuinely has, so a missing or indeterminate range is not clear. A
     * quality-control evaluation that does not exist is not an input at all: it
     * neither clears nor blocks, which is what lets an ordinary patient result,
     * which never carries one, clear at all.
     */
    public static boolean isClear(boolean rangeKnown, boolean inRange, String qcStatus, boolean nceOpen,
            boolean modified, boolean critical, boolean nonconforming, boolean ackPending) {
        return rangeKnown && inRange && !QC_FAIL.equals(qcStatus) && !nceOpen && !modified && !critical
                && !nonconforming && !ackPending;
    }

    /**
     * The rule on a queue row, evaluated server-side on the row the queue itself
     * served so a bulk release never trusts the client's list and the page never
     * derives a lane of its own (FR-5, FR-6).
     */
    public static boolean isClear(AnalysisItem row) {
        if (row == null) {
            return false;
        }
        boolean rangeKnown = !GenericValidator.isBlankOrNull(row.getNormalRange());
        return isClear(rangeKnown, row.isNormal(), row.getQcStatus(), row.isNceOpen(), row.isModified(),
                row.isCritical(), row.isNonconforming(), row.isAckPending());
    }

    /**
     * The same rule at result entry (OGC-1226 FR-7), where no queue row exists yet
     * and the range verdict comes from the result's own limit: a numeric value is
     * in range only inside an authored normal range, a select-list value only when
     * it is the authored expected-normal choice, and any other type has no range to
     * judge against. Automated validation may finalize a result unattended only
     * when this says clear, so automation never releases what the Clear lane would
     * hold.
     */
    public static boolean isClearAtEntry(ResultLimit limit, String resultType, String value, String qcStatus,
            boolean nceOpen, boolean modified, boolean nonconforming) {
        boolean rangeKnown = false;
        boolean inRange = false;
        boolean critical = false;
        if (limit != null && !GenericValidator.isBlankOrNull(limit.getId())) {
            if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.matches(resultType)) {
                rangeKnown = !GenericValidator.isBlankOrNull(limit.getDictionaryNormalId());
                inRange = rangeKnown && limit.getDictionaryNormalId().equals(value);
            } else if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(resultType)) {
                rangeKnown = Double.isFinite(limit.getLowNormal()) || Double.isFinite(limit.getHighNormal());
                String flag = resultFlag(limit, resultType, value);
                inRange = FLAG_NORMAL.equals(flag);
                critical = FLAG_CRITICAL.equals(flag);
            }
        }
        return isClear(rangeKnown, inRange, qcStatus, nceOpen, modified, critical, nonconforming, false);
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
