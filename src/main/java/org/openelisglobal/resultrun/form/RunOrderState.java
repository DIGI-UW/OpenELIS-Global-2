package org.openelisglobal.resultrun.form;

/**
 * Where an order stands for one test, as far as an analyzer value is concerned
 * (OGC-1200 FR-E4).
 */
public enum RunOrderState {
    /** An analysis of the test waits for a result the value can fill. */
    AWAITING,
    /** The test is ordered, and every analysis of it already has a result. */
    RESULTED,
    /** The test is not on the order (a cancelled analysis does not count). */
    NOT_ORDERED
}
