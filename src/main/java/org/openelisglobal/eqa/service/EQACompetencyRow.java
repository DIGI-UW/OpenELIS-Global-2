package org.openelisglobal.eqa.service;

import java.time.LocalDate;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;

/**
 * One assessable fact about an analyst in the competency window — either a
 * logged event or a scored result the log does not cover (FR-V2.3-06's union of
 * the two sources).
 *
 * <p>
 * Rows sharing a {@code participantResultId} are one fact, not several: an
 * unacceptable score that is then escalated writes two events about a single
 * sample, and counting both would fail the analyst twice for it.
 */
class EQACompetencyRow {

    /** Outcomes, worst last — the order the dashboard's "most recent" reads. */
    static final String ACCEPTABLE = "acceptable";
    static final String QUESTIONABLE = "questionable";
    static final String UNACCEPTABLE = "unacceptable";
    static final String MISSED = "missed";
    static final String DISMISSED = "dismissed";

    Long analystId;
    Long schemeId;
    String schemeName;
    Long analyteId;
    String analyteName;
    Long cycleId;
    Long participantResultId;
    LocalDate date;
    EQACompetencyEventType eventType;
    String outcome;

    /** In {@code evaluable_n} — the denominator of the band rules. */
    boolean counted;

    /** In {@code failure_n} — the numerator. */
    boolean failure;

    /** An escalation to a non-conformity, which the open-NCE band asks about. */
    boolean escalation;

    Integer nceId;

    /**
     * The grouping key for the FRS de-duplication. A row with no result behind it
     * (a cross-cycle event) is its own fact, so it keys on the event instead.
     */
    String factKey(int fallback) {
        return participantResultId != null ? "result:" + participantResultId : "event:" + fallback;
    }
}
