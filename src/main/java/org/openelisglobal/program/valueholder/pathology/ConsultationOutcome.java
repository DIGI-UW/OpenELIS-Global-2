package org.openelisglobal.program.valueholder.pathology;

/**
 * What the consulted pathologist concluded about the case's reading, stored as
 * a column of its own so the outcomes can be counted (AC-21).
 */
public enum ConsultationOutcome {

    AGREE,

    DISAGREE,

    /** Agrees in substance with a change to the reading. */
    MODIFIED
}
