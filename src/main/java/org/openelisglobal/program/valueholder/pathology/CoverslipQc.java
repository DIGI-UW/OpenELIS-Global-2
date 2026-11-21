package org.openelisglobal.program.valueholder.pathology;

/**
 * The quality check on a coverslipped slide. A slide not yet checked has no
 * result.
 */
public enum CoverslipQc {

    /** The coverslipped slide is fit to send to the pathologist. */
    PASS,

    /**
     * The slide failed the check; it is coverslipped again, or a new slide is cut
     * and this one retired.
     */
    FAIL
}
