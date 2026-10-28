package org.openelisglobal.batchworkplan.service;

import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;

/**
 * A batch could not be released as asked, with the QC standing that decided it
 * so the page can tell the user why (OGC-427 FR-NCE-001/005, BR-012).
 */
public class BatchReleaseRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        /** QC is not Pass and the request did not confirm an override. */
        OVERRIDE_REQUIRED,
        /** QC is in a state no override can release (no lot, retired lot). */
        BLOCKED,
        /** The caller confirmed an override but does not hold qa.qc.override. */
        OVERRIDE_NOT_PERMITTED
    }

    private final Reason reason;
    private final transient BatchQcStatusResponse qc;

    public BatchReleaseRefusedException(Reason reason, BatchQcStatusResponse qc) {
        super("Batch release refused: " + reason + " (QC " + (qc == null ? "unknown" : qc.status()) + ")");
        this.reason = reason;
        this.qc = qc;
    }

    public Reason getReason() {
        return reason;
    }

    public BatchQcStatusResponse getQc() {
        return qc;
    }
}
