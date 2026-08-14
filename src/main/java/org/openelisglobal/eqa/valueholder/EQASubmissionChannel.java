package org.openelisglobal.eqa.valueholder;

/**
 * How a participant result reached the provider. The FRS never enumerates this
 * formally; FHIR (FR-V2.2-04) and MANUAL (FR-V2.2-06 fallback, paired with
 * manual_submission_reference) are the two channels it describes.
 */
public enum EQASubmissionChannel {
    FHIR, MANUAL
}
