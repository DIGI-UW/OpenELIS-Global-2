package org.openelisglobal.sample.override.valueholder;

/**
 * Why the override was taken.
 *
 * The distinction matters downstream: an EQA proficiency sample legitimately
 * has no patient and is evaluated against the scheme's target value, whereas a
 * clinical order that goes patient-less has no evaluable reference range at
 * all. Only the recorded reason lets the two be told apart.
 */
public enum OverrideReasonCode {
    /** External quality assessment — a patient is not expected. */
    EQA,
    /** A user deliberately chose to proceed, and said why. */
    MANUAL
}
