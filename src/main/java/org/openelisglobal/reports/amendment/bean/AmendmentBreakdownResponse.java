package org.openelisglobal.reports.amendment.bean;

import java.util.List;

public class AmendmentBreakdownResponse {

    /** Only tests with at least one amendment, amendedCount DESC then testName. */
    private List<BreakdownRow> rows;

    public List<BreakdownRow> getRows() {
        return rows;
    }

    public void setRows(List<BreakdownRow> rows) {
        this.rows = rows;
    }

    public static class BreakdownRow {
        private String testName;
        private long amendedCount;
        private long releasedCount;
        /** Percent, 2 decimals; null when no release of this test in the window. */
        private Double ratePercent;

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
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
