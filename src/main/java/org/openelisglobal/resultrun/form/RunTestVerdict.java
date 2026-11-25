package org.openelisglobal.resultrun.form;

/**
 * A run's verdict for one test it covers (OGC-1200 FR-A4, FR-C3): Pass only if
 * every control covering the test passes, Fail if any fails, No control if none
 * covers it. Until the per-test policy exists (FR-H1) a control is not
 * required, so a test with no control is Not required.
 */
public enum RunTestVerdict {
    PASS, FAIL, NO_CONTROL, NOT_REQUIRED
}
