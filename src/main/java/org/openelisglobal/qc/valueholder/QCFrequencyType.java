package org.openelisglobal.qc.valueholder;

/**
 * How often a control lot must pass QC before patient work on it is released
 * (OGC-427). The window answers "is QC current enough?"; Westgard evaluation
 * answers "is QC valid?". A batch needs both.
 */
public enum QCFrequencyType {

    /** A passing run on the current calendar day. */
    DAILY,

    /** A passing run within the lot's shift length, eight hours by default. */
    PER_SHIFT,

    /** A passing run within the lot's configured number of hours. */
    CUSTOM_HOURS,

    /** A passing run since the last batch released against the lot. */
    PER_RUN
}
