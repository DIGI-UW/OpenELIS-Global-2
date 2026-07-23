package org.openelisglobal.reports.tat.bean;

import java.math.BigDecimal;
import java.util.List;

public class TATSummaryResponse {

    private String calculationMode;
    private int excludedDaysCount;
    private int totalCount;
    private BigDecimal mean;
    private BigDecimal median;
    private BigDecimal percentile90;
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal stdDeviation;
    private List<HistogramBin> histogram;
    private List<BreakdownRow> breakdown;

    // Getters and setters
    public String getCalculationMode() {
        return calculationMode;
    }

    public void setCalculationMode(String calculationMode) {
        this.calculationMode = calculationMode;
    }

    public int getExcludedDaysCount() {
        return excludedDaysCount;
    }

    public void setExcludedDaysCount(int excludedDaysCount) {
        this.excludedDaysCount = excludedDaysCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public BigDecimal getMean() {
        return mean;
    }

    public void setMean(BigDecimal mean) {
        this.mean = mean;
    }

    public BigDecimal getMedian() {
        return median;
    }

    public void setMedian(BigDecimal median) {
        this.median = median;
    }

    public BigDecimal getPercentile90() {
        return percentile90;
    }

    public void setPercentile90(BigDecimal percentile90) {
        this.percentile90 = percentile90;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public BigDecimal getStdDeviation() {
        return stdDeviation;
    }

    public void setStdDeviation(BigDecimal stdDeviation) {
        this.stdDeviation = stdDeviation;
    }

    public List<HistogramBin> getHistogram() {
        return histogram;
    }

    public void setHistogram(List<HistogramBin> histogram) {
        this.histogram = histogram;
    }

    public List<BreakdownRow> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<BreakdownRow> breakdown) {
        this.breakdown = breakdown;
    }

    public static class HistogramBin {
        private String binLabel;
        private BigDecimal binMin;
        private BigDecimal binMax;
        private int count;

        public HistogramBin(String binLabel, BigDecimal binMin, BigDecimal binMax, int count) {
            this.binLabel = binLabel;
            this.binMin = binMin;
            this.binMax = binMax;
            this.count = count;
        }

        public String getBinLabel() {
            return binLabel;
        }

        public BigDecimal getBinMin() {
            return binMin;
        }

        public BigDecimal getBinMax() {
            return binMax;
        }

        public int getCount() {
            return count;
        }
    }

    public static class BreakdownRow {
        private String dimensionValue;
        private Integer dimensionId;
        private int count;
        private BigDecimal mean;
        private BigDecimal median;
        private BigDecimal percentile90;
        private BigDecimal max;

        public String getDimensionValue() {
            return dimensionValue;
        }

        public void setDimensionValue(String dimensionValue) {
            this.dimensionValue = dimensionValue;
        }

        public Integer getDimensionId() {
            return dimensionId;
        }

        public void setDimensionId(Integer dimensionId) {
            this.dimensionId = dimensionId;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public BigDecimal getMean() {
            return mean;
        }

        public void setMean(BigDecimal mean) {
            this.mean = mean;
        }

        public BigDecimal getMedian() {
            return median;
        }

        public void setMedian(BigDecimal median) {
            this.median = median;
        }

        public BigDecimal getPercentile90() {
            return percentile90;
        }

        public void setPercentile90(BigDecimal percentile90) {
            this.percentile90 = percentile90;
        }

        public BigDecimal getMax() {
            return max;
        }

        public void setMax(BigDecimal max) {
            this.max = max;
        }
    }
}
