package org.openelisglobal.eqa.valueholder;

/**
 * How a provider cycle reaches its participants and returns their scores
 * (FR-V2.5-02 step 4). FHIR is the wired channel, CSV the export fallback, and
 * MIXED the honest answer when some participants are on FHIR and the rest take
 * a file — the receipt monitor and score distribution read this to decide which
 * path a participant gets.
 */
public enum EQADistributionMethod {
    FHIR, CSV, MIXED
}
