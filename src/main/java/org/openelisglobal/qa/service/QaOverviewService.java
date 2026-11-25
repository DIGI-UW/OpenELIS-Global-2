package org.openelisglobal.qa.service;

import org.openelisglobal.qa.dto.QaOverviewSummary;

/**
 * Aggregation service for the QA Overview page (OGC-694).
 */
public interface QaOverviewService {

    /**
     * Compile the QC, EQA, audit, e-signature, and activity-feed numbers the QA
     * Overview needs in a single call.
     */
    QaOverviewSummary getSummary();
}
