package org.openelisglobal.biorepository.service;

/**
 * Metadata from phased fulfillment sample search.
 */
public class FulfillmentSearchMetadata {

    private boolean hasExactIdentityInput;
    private boolean exactIdentityMatchesFound;
    private boolean fallbackUsed;
    private boolean noExactMatch;
    private int searchPhase;

    public boolean isHasExactIdentityInput() {
        return hasExactIdentityInput;
    }

    public void setHasExactIdentityInput(boolean hasExactIdentityInput) {
        this.hasExactIdentityInput = hasExactIdentityInput;
    }

    public boolean isExactIdentityMatchesFound() {
        return exactIdentityMatchesFound;
    }

    public void setExactIdentityMatchesFound(boolean exactIdentityMatchesFound) {
        this.exactIdentityMatchesFound = exactIdentityMatchesFound;
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

    public int getSearchPhase() {
        return searchPhase;
    }

    public void setSearchPhase(int searchPhase) {
        this.searchPhase = searchPhase;
    }
}
