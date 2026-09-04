package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Writes the immutable Manual Entry submission audit (US4 / FR-008).
 * Insert-only — re-submitting a week produces a new distinct row; there is no
 * update or delete path.
 */
public interface ManualEntrySubmissionService {

    /**
     * Record a "mark week submitted" action. Serialises {@code valueSnapshot} to
     * JSON and inserts one immutable audit row.
     *
     * @return the persisted audit row (with generated id)
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    ManualEntrySubmissionAudit submit(LocalDate periodStart, LocalDate periodEnd, Integer siteId,
            Map<String, String> valueSnapshot, String sysUserId);

    /** Submission history, newest first, optionally filtered by period. */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ManualEntrySubmissionAudit> getAudit(LocalDate periodStart, LocalDate periodEnd);
}
