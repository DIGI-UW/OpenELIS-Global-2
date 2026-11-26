package org.openelisglobal.resultrun.form;

/**
 * A run's verdict for one test it covers (OGC-1200 FR-C3, FR-C12): Fail if a
 * control fails, or a blank or duplicate fails under a Hold policy; No control
 * if a kind the test requires per run is missing; Pass with warning if the only
 * failures are blanks or duplicates under a Warn policy; Pass if what was
 * recorded passes; Not required if nothing is required and nothing recorded.
 */
public enum RunTestVerdict {
    PASS, PASS_WITH_WARNING, FAIL, NO_CONTROL, NOT_REQUIRED
}
