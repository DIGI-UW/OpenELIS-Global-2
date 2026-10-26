package org.openelisglobal.analyzerresults.service;

/** A reviewer's decisions on an analyzer result held from review. */
public interface AnalyzerHeldResultService {

    /**
     * Removes a held result from review for good, recorded in the audit trail as
     * the reviewer's. A result that is not held is refused.
     */
    void dismiss(String stagedResultId, String actor);
}
