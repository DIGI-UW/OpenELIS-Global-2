package org.openelisglobal.eqa.service;

/**
 * OGC-933 — the printed CPHL-format EQA performance report. Section- and
 * programme-level summaries, the z-score / performance scoring table and the
 * cycle identifiers, in the layout that replaces CPHL's Access report.
 */
public interface EQAPerformanceReportPDFService {

    /** Renders one cycle's report. Unsubmitted (DRAFT) results are excluded. */
    byte[] generatePerformanceReport(Long cycleId);
}
