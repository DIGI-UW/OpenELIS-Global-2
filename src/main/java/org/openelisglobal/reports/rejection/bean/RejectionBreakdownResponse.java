package org.openelisglobal.reports.rejection.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Two groupings of the same window: rejections by reason (the Pareto — reason
 * text is the REJECTION_REASONS dictionary value snapshotted onto the note at
 * write time) and by test (with per-test rates against analyses started).
 */
public class RejectionBreakdownResponse {

    private List<ReasonRow> reasons = new ArrayList<>();
    private List<TestRow> tests = new ArrayList<>();

    public List<ReasonRow> getReasons() {
        return reasons;
    }

    public void setReasons(List<ReasonRow> reasons) {
        this.reasons = reasons;
    }

    public List<TestRow> getTests() {
        return tests;
    }

    public void setTests(List<TestRow> tests) {
        this.tests = tests;
    }

    public static class ReasonRow {

        private String reason;
        private long count;
        private Double percentOfRejections;
        private Double cumulativePercent;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public Double getPercentOfRejections() {
            return percentOfRejections;
        }

        public void setPercentOfRejections(Double percentOfRejections) {
            this.percentOfRejections = percentOfRejections;
        }

        public Double getCumulativePercent() {
            return cumulativePercent;
        }

        public void setCumulativePercent(Double cumulativePercent) {
            this.cumulativePercent = cumulativePercent;
        }
    }

    public static class TestRow {

        private String testName;
        private long rejectedCount;
        private long totalCount;
        private Double ratePercent;

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
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
