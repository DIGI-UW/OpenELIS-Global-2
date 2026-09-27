package org.openelisglobal.sampleacceptance.service;

/**
 * Outcome of a Resample commit: the rejected original, the new draft order, and
 * the NCE.
 */
public class ResampleResult {

    private final String originalSampleId;
    private final String newSampleId;
    private final String newAccessionNumber;
    private final Integer nceId;

    public ResampleResult(String originalSampleId, String newSampleId, String newAccessionNumber, Integer nceId) {
        this.originalSampleId = originalSampleId;
        this.newSampleId = newSampleId;
        this.newAccessionNumber = newAccessionNumber;
        this.nceId = nceId;
    }

    public String getOriginalSampleId() {
        return originalSampleId;
    }

    public String getNewSampleId() {
        return newSampleId;
    }

    public String getNewAccessionNumber() {
        return newAccessionNumber;
    }

    public Integer getNceId() {
        return nceId;
    }
}
