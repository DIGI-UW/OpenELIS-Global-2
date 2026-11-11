package org.openelisglobal.reports.amendment.bean;

import java.util.List;

public class AmendmentTrendResponse {

    private List<TrendPoint> points;

    public List<TrendPoint> getPoints() {
        return points;
    }

    public void setPoints(List<TrendPoint> points) {
        this.points = points;
    }

    public static class TrendPoint {
        /**
         * Period key: "2025-07-07" (DAILY), "2025-W28" (WEEKLY), "2025-07" (MONTHLY).
         */
        private String period;
        private long amendedCount;
        private long releasedCount;
        /** Percent, 2 decimals; null when nothing was released in the period. */
        private Double ratePercent;

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

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
}
