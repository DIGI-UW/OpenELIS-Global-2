package org.openelisglobal.eqa.service;

/**
 * OGC-933 — the printed CPHL-format EQA performance report. Section- and
 * programme-level summaries, the z-score / performance scoring table and the
 * cycle identifiers, in the layout that replaces CPHL's Access report.
 */
public interface EQAPerformanceReportPDFService {

    /**
     * Renders one cycle's report.
     *
     * @param labEnrollmentId narrows the report to a single participant enrollment,
     *                        for the provider-side per-participant variant; null
     *                        reports the whole cycle
     */
    byte[] generatePerformanceReport(Long cycleId, Long labEnrollmentId);
}
