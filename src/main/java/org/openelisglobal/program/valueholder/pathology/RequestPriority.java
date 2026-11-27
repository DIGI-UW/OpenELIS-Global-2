package org.openelisglobal.program.valueholder.pathology;

/**
 * How soon the bench is asked to do a pathologist's request (FR-10.6). A
 * request on file from before priorities were recorded has none.
 */
public enum RequestPriority {

    /**
     * Done in the bench's usual order; a request raised with no priority is this.
     */
    NORMAL,

    /** Done ahead of the usual order. */
    URGENT,

    /** Done at once. */
    STAT
}
