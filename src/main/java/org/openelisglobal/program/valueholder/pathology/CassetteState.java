package org.openelisglobal.program.valueholder.pathology;

/**
 * A cassette and the block it becomes are one row, and this is the point that
 * row has reached.
 */
public enum CassetteState {

    /** Tissue has been cut into the cassette and is waiting to be embedded. */
    CASSETTE,

    /** The same row after embedding, now the block that slides are cut from. */
    BLOCK
}
