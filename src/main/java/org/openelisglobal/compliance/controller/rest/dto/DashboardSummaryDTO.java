package org.openelisglobal.compliance.controller.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryDTO {
    private int totalOrders;
    private double complianceRate;
    private int totalExceedances;
    private int sitesMonitored;
    private TrendDTO trend;

    public static class TrendDTO {
        private int totalOrders;
        private double complianceRate;
        private int totalExceedances;
        private int sitesMonitored;
        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int v) { totalOrders = v; }
        public double getComplianceRate() { return complianceRate; }
        public void setComplianceRate(double v) { complianceRate = v; }
        public int getTotalExceedances() { return totalExceedances; }
        public void setTotalExceedances(int v) { totalExceedances = v; }
        public int getSitesMonitored() { return sitesMonitored; }
        public void setSitesMonitored(int v) { sitesMonitored = v; }
    }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int v) { totalOrders = v; }
    public double getComplianceRate() { return complianceRate; }
    public void setComplianceRate(double v) { complianceRate = v; }
    public int getTotalExceedances() { return totalExceedances; }
    public void setTotalExceedances(int v) { totalExceedances = v; }
    public int getSitesMonitored() { return sitesMonitored; }
    public void setSitesMonitored(int v) { sitesMonitored = v; }
    public TrendDTO getTrend() { return trend; }
    public void setTrend(TrendDTO v) { trend = v; }
}
