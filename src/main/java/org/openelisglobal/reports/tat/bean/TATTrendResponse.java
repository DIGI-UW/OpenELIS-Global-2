package org.openelisglobal.reports.tat.bean;

import java.math.BigDecimal;
import java.util.List;

public class TATTrendResponse {

    private String calculationMode;
    private List<TrendSeries> series;

    public String getCalculationMode() {
        return calculationMode;
    }

    public void setCalculationMode(String calculationMode) {
        this.calculationMode = calculationMode;
    }

    public List<TrendSeries> getSeries() {
        return series;
    }

    public void setSeries(List<TrendSeries> series) {
        this.series = series;
    }

    public static class TrendSeries {
        private String label;
        private List<TrendDataPoint> dataPoints;

        public TrendSeries(String label, List<TrendDataPoint> dataPoints) {
            this.label = label;
            this.dataPoints = dataPoints;
        }

        public String getLabel() {
            return label;
        }

        public List<TrendDataPoint> getDataPoints() {
            return dataPoints;
        }
    }

    public static class TrendDataPoint {
        private String period;
        private BigDecimal mean;
        private BigDecimal median;
        private BigDecimal percentile90;
        private int count;

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
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

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
