package org.openelisglobal.reports.rejection.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat ordering-location × test-section cells; the frontend pivots them into
 * the heatmap grid. {@code location} is null when the sample carries no
 * requesting-organization — the UI labels that bucket, not the backend.
 */
public class RejectionHeatmapResponse {

    private List<Cell> cells = new ArrayList<>();

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public static class Cell {

        private String location;
        private String section;
        private long totalCount;
        private long rejectedCount;
        private Double ratePercent;

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getRejectedCount() {
            return rejectedCount;
        }

        public void setRejectedCount(long rejectedCount) {
            this.rejectedCount = rejectedCount;
        }

        public Double getRatePercent() {
            return ratePercent;
        }

        public void setRatePercent(Double ratePercent) {
            this.ratePercent = ratePercent;
        }
    }
}
