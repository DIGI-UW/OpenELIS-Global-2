package org.openelisglobal.qaevent.service;

import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Auto-creates a Non-Conforming Event from a REJECTION-severity Westgard QC
 * violation (NCE FRS trigger #10, OGC-701) and links the affected patient
 * samples within a capped lookback window (OGC-728).
 */
public interface QcViolationNceService {

    /**
     * Create the NCE for a QC violation, linking affected patient analyses.
     * Idempotent: if an NCE already exists for this violation, it is returned
     * unchanged.
     */
    NcEvent createNceForViolation(QCRuleViolation violation);
}
