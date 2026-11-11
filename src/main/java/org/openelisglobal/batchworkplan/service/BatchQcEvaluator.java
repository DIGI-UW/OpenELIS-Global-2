package org.openelisglobal.batchworkplan.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCFrequencyType;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * The QC standing of one control lot for a batch release (OGC-427 FR-QC-003,
 * BR-002). It reads what the Westgard pipeline has already decided, the result
 * status and the open violations, and adds only the frequency window; it never
 * evaluates a rule itself (BR-007).
 *
 * <p>
 * Precedence, most severe first: the lot's lifecycle, an open rejection
 * violation, a rejected latest result, a passing result outside the frequency
 * window, and no result at all.
 */
public final class BatchQcEvaluator {

    static final String LOT_ACTIVE = "ACTIVE";
    static final String LOT_ESTABLISHMENT = "ESTABLISHMENT";
    static final String RESULT_ACCEPTED = "ACCEPTED";
    static final String RESULT_REJECTED = "REJECTED";

    private static final int DEFAULT_SHIFT_HOURS = 8;
    private static final int DEFAULT_CUSTOM_HOURS = 24;

    private BatchQcEvaluator() {
    }

    /**
     * @param latestFinal    the lot's most recent ACCEPTED or REJECTED result, or
     *                       null when it has none
     * @param latestAccepted the lot's most recent ACCEPTED result, or null
     * @param openRejections unresolved REJECTION-severity violations on the lot
     * @param lastRelease    when a batch was last released against the lot, or
     *                       null; only a PER_RUN lot reads it
     */
    public static BatchQcStatus evaluate(QCControlLot lot, QCResult latestFinal, QCResult latestAccepted,
            List<QCRuleViolation> openRejections, Timestamp lastRelease, ZonedDateTime now) {
        if (isRetired(lot, now)) {
            return BatchQcStatus.LOT_EXPIRED;
        }
        if (LOT_ESTABLISHMENT.equals(lot.getStatus())) {
            return BatchQcStatus.ESTABLISHMENT;
        }
        if (Boolean.FALSE.equals(lot.getQcRequired())) {
            return BatchQcStatus.PASS;
        }
        if (openRejections != null && !openRejections.isEmpty()) {
            return BatchQcStatus.VIOLATION;
        }
        if (latestFinal == null) {
            return BatchQcStatus.NOT_RUN;
        }
        if (RESULT_REJECTED.equals(latestFinal.getResultStatus())) {
            return BatchQcStatus.FAILED;
        }
        if (latestAccepted == null || !withinWindow(lot, latestAccepted.getRunDateTime(), lastRelease, now)) {
            return BatchQcStatus.OVERDUE;
        }
        return BatchQcStatus.PASS;
    }

    /**
     * A lot no new run can be recorded against: anything that is neither ACTIVE nor
     * ESTABLISHMENT, and a lot whose expiration date has passed even though no one
     * has retired it yet.
     */
    public static boolean isRetired(QCControlLot lot, ZonedDateTime now) {
        if (!LOT_ACTIVE.equals(lot.getStatus()) && !LOT_ESTABLISHMENT.equals(lot.getStatus())) {
            return true;
        }
        Timestamp expiration = lot.getExpirationDate();
        return expiration != null && !expiration.toInstant().isAfter(now.toInstant());
    }

    /**
     * Whether a passing run at {@code runAt} still covers a release at {@code now}.
     * A run dated in the future covers nothing: it is a data-entry error, not
     * evidence that QC was done.
     */
    public static boolean withinWindow(QCControlLot lot, Timestamp runAt, Timestamp lastRelease, ZonedDateTime now) {
        if (runAt == null || runAt.toInstant().isAfter(now.toInstant())) {
            return false;
        }
        QCFrequencyType type = lot.getQcFrequencyType() == null ? QCFrequencyType.DAILY : lot.getQcFrequencyType();
        switch (type) {
        case PER_SHIFT:
            return withinHours(runAt, positiveOr(lot.getShiftDefinitionHours(), DEFAULT_SHIFT_HOURS), now);
        case CUSTOM_HOURS:
            return withinHours(runAt, positiveOr(lot.getQcFrequencyHours(), DEFAULT_CUSTOM_HOURS), now);
        case PER_RUN:
            return lastRelease == null || runAt.after(lastRelease);
        case DAILY:
        default:
            ZoneId zone = now.getZone();
            return runAt.toInstant().atZone(zone).toLocalDate().equals(now.toLocalDate());
        }
    }

    /**
     * When the current passing run stops covering releases, or null when that is
     * not a point in time (PER_RUN, or no passing run).
     */
    public static ZonedDateTime nextDue(QCControlLot lot, Timestamp runAt, ZonedDateTime now) {
        if (runAt == null) {
            return null;
        }
        ZonedDateTime ranAt = runAt.toInstant().atZone(now.getZone());
        QCFrequencyType type = lot.getQcFrequencyType() == null ? QCFrequencyType.DAILY : lot.getQcFrequencyType();
        switch (type) {
        case PER_SHIFT:
            return ranAt.plusHours(positiveOr(lot.getShiftDefinitionHours(), DEFAULT_SHIFT_HOURS));
        case CUSTOM_HOURS:
            return ranAt.plusHours(positiveOr(lot.getQcFrequencyHours(), DEFAULT_CUSTOM_HOURS));
        case PER_RUN:
            return null;
        case DAILY:
        default:
            return ranAt.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
        }
    }

    private static boolean withinHours(Timestamp runAt, int hours, ZonedDateTime now) {
        return Duration.between(runAt.toInstant(), now.toInstant()).compareTo(Duration.ofHours(hours)) <= 0;
    }

    private static int positiveOr(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }
}
