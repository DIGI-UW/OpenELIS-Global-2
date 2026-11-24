package org.openelisglobal.resultrun.form;

/**
 * The run records an analyzer value cannot be accepted from until someone
 * resolves them (OGC-1200 FR-E4).
 */
public enum ResultRunExceptionKind {
    /** The value's lab number names no order. */
    UNMATCHED,
    /** The order exists, and the value's test is not ordered on it. */
    UNORDERED,
    /** The analyzer's code or answer has no test or result mapped to it. */
    UNMAPPED
}
