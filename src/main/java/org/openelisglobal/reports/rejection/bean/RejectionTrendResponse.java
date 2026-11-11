package org.openelisglobal.reports.rejection.bean;

import java.util.ArrayList;
import java.util.List;

public class RejectionTrendResponse {

    private List<TrendPoint> points = new ArrayList<>();

    public List<TrendPoint> getPoints() {
        return points;
    }

    public void setPoints(List<TrendPoint> points) {
        this.points = points;
    }

    public static class TrendPoint {

        private String period;
        private long rejectedCount;
        private long totalCount;
        private Double ratePercent;

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public long getRejectedCount() {
            return rejectedCount;
        }

        public void setRejectedCount(long rejectedCount) {
            this.rejectedCount = rejectedCount;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public Double getRatePercent() {
            return ratePercent;
        }

        public void setRatePercent(Double ratePercent) {
            this.ratePercent = ratePercent;
        }
    }
}
