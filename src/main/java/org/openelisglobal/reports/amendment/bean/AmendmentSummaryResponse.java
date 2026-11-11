package org.openelisglobal.reports.amendment.bean;

public class AmendmentSummaryResponse {

    private long amendedCount;
    private long releasedCount;
    /** Percent, 2 decimals; null when nothing was released in the window. */
    private Double ratePercent;

    public long getAmendedCount() {
        return amendedCount;
    }

    public void setAmendedCount(long amendedCount) {
        this.amendedCount = amendedCount;
    }

    public long getReleasedCount() {
        return releasedCount;
    }

    public void setReleasedCount(long releasedCount) {
        this.releasedCount = releasedCount;
    }

    public Double getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(Double ratePercent) {
        this.ratePercent = ratePercent;
    }
}
