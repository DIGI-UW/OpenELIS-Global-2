package org.openelisglobal.compliance.controller.rest.dto;

public class MonthDataPointDTO {
    private String month;
    private double complianceRate;
    private int totalResults;
    private boolean lowData;

    public String getMonth() {
        return month;
    }

    public void setMonth(String v) {
        month = v;
    }

    public double getComplianceRate() {
        return complianceRate;
    }

    public void setComplianceRate(double v) {
        complianceRate = v;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int v) {
        totalResults = v;
    }

    public boolean isLowData() {
        return lowData;
    }

    public void setLowData(boolean v) {
        lowData = v;
    }
}
