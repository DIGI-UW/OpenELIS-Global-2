package org.openelisglobal.eqa.valueholder;

/**
 * How a participant result reached the provider. The specification never
 * enumerates this formally; FHIR and MANUAL (the fallback, paired with
 * manual_submission_reference) are the two channels it describes.
 */
public enum EQASubmissionChannel {
    FHIR, MANUAL
}
