package org.openelisglobal.program.valueholder.pathology;

/**
 * Where a second opinion stands (FR-11). Only an awaited one is answered or
 * withdrawn; the other two are final.
 */
public enum ConsultationStatus {

    /** Asked for, and the consulted pathologist has not answered yet. */
    REQUESTED,

    /** The consulted pathologist recorded an outcome. */
    ANSWERED,

    /** No longer wanted; the row is kept. */
    WITHDRAWN
}
