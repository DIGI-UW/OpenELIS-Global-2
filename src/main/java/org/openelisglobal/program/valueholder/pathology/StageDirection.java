package org.openelisglobal.program.valueholder.pathology;

/** Which way a case moved when it entered a stage. */
public enum StageDirection {
    /** Sent on to a later stage of the bench. */
    FORWARD,
    /** Sent back to an earlier stage, with a reason. */
    RETURN
}
