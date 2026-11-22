package org.openelisglobal.qaevent.service;

import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.qaevent.valueholder.NcEvent;

/**
 * Auto-creates a non-conforming event for a test rejection (OGC-710 follow-on
 * to triggers #10/#12): every REJECTION_REASON note persisted at results entry
 * opens one MINOR NCE per rejected analysis, keyed (TEST_REJECTION, analysisId)
 * for idempotency.
 */
public interface TestRejectionNceService {

    NcEvent createForRejection(Note rejectionNote);
}
