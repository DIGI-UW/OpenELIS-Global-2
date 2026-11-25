package org.openelisglobal.qaevent.service;

import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Auto-creates a Non-Conforming Event from a REJECTION-severity Westgard QC
 * violation (OGC-701) and links the affected patient samples within a capped
 * lookback window (OGC-728).
 */
public interface QcViolationNceService {

    /**
     * Create the NCE for a QC violation, linking affected patient analyses.
     * Idempotent: if an NCE already exists for this violation, it is returned
     * unchanged.
     */
    NcEvent createNceForViolation(QCRuleViolation violation);

    /**
     * Create the NCE for a failed bench control that has no statistical violation
     * behind it — an RDT Invalid control line (OGC-1147). Scopes affected analyses
     * by lab unit rather than analyzer, using the same capped window. Idempotent on
     * the QC result.
     */
    NcEvent createNceForFailedControl(QCResult qcResult);

    /**
     * OGC-1200 FR-C5, FR-G6: the non-conformity a decision on a held test in a run
     * files (mandatory for Accept despite, offered on Reject), pre-populated from
     * the run and linked to the test's analyses in it. Its trigger is the decision,
     * so it never counts as a second QC hold.
     */
    NcEvent createNceForRunDecision(String decisionId, String title, String description,
            java.util.Collection<String> analysisIds, String sysUserId);
}
