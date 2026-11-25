package org.openelisglobal.resultrun.form;

/** Why a run holds a test's rows (OGC-1200 FR-A3, FR-D4). */
public enum RunHoldCause {
    /** A control covering the test failed on the run. */
    QC_FAILED,
    /**
     * The test requires a control per run, none is recorded, and its policy holds
     * when one is missing (FR-H1, FR-C7).
     */
    QC_MISSING
}
