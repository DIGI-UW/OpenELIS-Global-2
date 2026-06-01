package org.openelisglobal.biorepository.controller.rest.dto;

/**
 * Stable workbench-ready summary for a retrieval suggestion row.
 */
public class RetrievalItemSuggestionSummaryDTO {

    private String sampleIdentity;
    private Double availableQuantity;
    private String availableUnitOfMeasure;
    private String samplePath;
    private String matchReason;
    private Integer matchScore;
    private Boolean sampleTypeMatchesRequested;
    private String mismatchReason;

    public String getSampleIdentity() {
        return sampleIdentity;
    }

    public void setSampleIdentity(String sampleIdentity) {
        this.sampleIdentity = sampleIdentity;
    }

    public Double getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Double availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getAvailableUnitOfMeasure() {
        return availableUnitOfMeasure;
    }

    public void setAvailableUnitOfMeasure(String availableUnitOfMeasure) {
        this.availableUnitOfMeasure = availableUnitOfMeasure;
    }

    public String getSamplePath() {
        return samplePath;
    }

    public void setSamplePath(String samplePath) {
        this.samplePath = samplePath;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public Boolean getSampleTypeMatchesRequested() {
        return sampleTypeMatchesRequested;
    }

    public void setSampleTypeMatchesRequested(Boolean sampleTypeMatchesRequested) {
        this.sampleTypeMatchesRequested = sampleTypeMatchesRequested;
    }

    public String getMismatchReason() {
        return mismatchReason;
    }

    public void setMismatchReason(String mismatchReason) {
        this.mismatchReason = mismatchReason;
    }
}
