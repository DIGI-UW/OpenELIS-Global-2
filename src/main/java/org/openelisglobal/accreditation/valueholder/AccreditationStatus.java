package org.openelisglobal.accreditation.valueholder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * OGC-686 — the status chip shown for an accrediting body.
 *
 * <p>
 * Derived, never stored: it is a pure function of the body's {@code active}
 * flag and its {@code expires_on} date. Because expiry lives on the body (see
 * {@link AccreditingBody}), a body has exactly one status — there is no
 * per-enrolled-test status and no "majority of rows" heuristic.
 *
 * <p>
 * The {@value #EXPIRING_WINDOW_DAYS}-day warning window matches FRS FR-15/17/22
 * and the mockup. One constant, one place, so the page chip, the summary banner
 * and the QA Overview answer can never drift apart.
 */
public enum AccreditationStatus {

    /** Not active — excluded from report logos and the notes line entirely. */
    INACTIVE,

    /** Expiry already past. */
    EXPIRED,

    /** Expires within the warning window. Still valid for report rendering. */
    EXPIRING,

    /** Active and comfortably in date. */
    ACTIVE;

    public static final int EXPIRING_WINDOW_DAYS = 60;

    /**
     * @param active    the body's active flag (null treated as inactive)
     * @param expiresOn the body's certificate expiry (null treated as expired — the
     *                  column is NOT NULL, so this only guards bad input)
     * @param asOf      the date to evaluate against; callers pass the report's
     *                  release date for rendering, or today for the admin view
     */
    public static AccreditationStatus of(Boolean active, LocalDate expiresOn, LocalDate asOf) {
        if (!Boolean.TRUE.equals(active)) {
            return INACTIVE;
        }
        if (expiresOn == null) {
            return EXPIRED;
        }
        if (expiresOn.isBefore(asOf)) {
            return EXPIRED;
        }
        if (ChronoUnit.DAYS.between(asOf, expiresOn) <= EXPIRING_WINDOW_DAYS) {
            return EXPIRING;
        }
        return ACTIVE;
    }

}
