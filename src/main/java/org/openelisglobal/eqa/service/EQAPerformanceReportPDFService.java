package org.openelisglobal.eqa.service;

/**
 * OGC-933 — the printed EQA performance report. Section- and programme-level
 * summaries, the z-score / performance scoring table and the cycle identifiers,
 * in the layout a national provider's paper report uses.
 */
public interface EQAPerformanceReportPDFService {

    /** Renders one cycle's report. Unsubmitted (DRAFT) results are excluded. */
    byte[] generatePerformanceReport(Long cycleId);

    /**
     * Renders the report a provider sends one participating laboratory, from the
     * results that laboratory reported into this cycle. Same layout as the report a
     * laboratory prints for itself, minus the two columns that belong to the
     * reporting laboratory rather than to the provider judging it.
     *
     * @param cycleId        the provider cycle
     * @param organizationId the participating laboratory
     */
    byte[] generateParticipantPerformanceReport(Long cycleId, Long organizationId);
}
