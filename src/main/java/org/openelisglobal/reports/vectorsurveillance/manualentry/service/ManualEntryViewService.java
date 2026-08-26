package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import java.time.LocalDate;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryViewDTO;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Composes the Manual Entry Helper view (US4): field-map rows (ordered,
 * visible) each carrying the metric value derived from the M1
 * {@code VectorSurveillanceService.getIndices(...)} for the period. The
 * sporozoite row is flagged low-confidence (rate shown with a caveat, not
 * withheld) when {@code positiveResolutionPct < 95%}.
 */
public interface ManualEntryViewService {

    /**
     * Build the helper view for the reporting period. {@code siteId} null =
     * lab-level.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ManualEntryViewDTO getView(LocalDate periodStart, LocalDate periodEnd, Integer siteId);
}
