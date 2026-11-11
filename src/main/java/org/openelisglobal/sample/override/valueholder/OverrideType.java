package org.openelisglobal.sample.override.valueholder;

/** The controls an order can be deliberately taken past. */
public enum OverrideType {
    /** Ordered without a patient, so no reference range can be evaluated. */
    NO_PATIENT,
    /** Testing continued after an intake acceptance check failed. */
    CONTINUE_WITH_TESTING
}
