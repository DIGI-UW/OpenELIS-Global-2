package org.openelisglobal.biorepository.controller.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestion summary for one retrieval reference line.
 */
public class RetrievalItemSuggestionDTO {

    private Integer retrievalItemId;
    private String suggestionStatus;
    private boolean exactMatchFound;
    private boolean fallbackUsed;
    private boolean noExactMatch;
    private boolean sampleTypeMatchesRequested = true;
    private String mismatchReason;
    private BioSampleListDTO topCandidate;
    private RetrievalItemSuggestionSummaryDTO summary;
    private List<BioSampleListDTO> candidates = new ArrayList<>();

    public Integer getRetrievalItemId() {
        return retrievalItemId;
    }

    public void setRetrievalItemId(Integer retrievalItemId) {
        this.retrievalItemId = retrievalItemId;
    }

    public String getSuggestionStatus() {
        return suggestionStatus;
    }

    public void setSuggestionStatus(String suggestionStatus) {
        this.suggestionStatus = suggestionStatus;
    }

    public boolean isExactMatchFound() {
        return exactMatchFound;
    }

    public void setExactMatchFound(boolean exactMatchFound) {
        this.exactMatchFound = exactMatchFound;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public boolean isNoExactMatch() {
        return noExactMatch;
    }

    public void setNoExactMatch(boolean noExactMatch) {
        this.noExactMatch = noExactMatch;
    }

    public boolean isSampleTypeMatchesRequested() {
        return sampleTypeMatchesRequested;
    }

    public void setSampleTypeMatchesRequested(boolean sampleTypeMatchesRequested) {
        this.sampleTypeMatchesRequested = sampleTypeMatchesRequested;
    }

    public String getMismatchReason() {
        return mismatchReason;
    }

    public void setMismatchReason(String mismatchReason) {
        this.mismatchReason = mismatchReason;
    }

    public BioSampleListDTO getTopCandidate() {
        return topCandidate;
    }

    public void setTopCandidate(BioSampleListDTO topCandidate) {
        this.topCandidate = topCandidate;
    }

    public RetrievalItemSuggestionSummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(RetrievalItemSuggestionSummaryDTO summary) {
        this.summary = summary;
    }

    public List<BioSampleListDTO> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<BioSampleListDTO> candidates) {
        this.candidates = candidates != null ? candidates : new ArrayList<>();
    }
}
