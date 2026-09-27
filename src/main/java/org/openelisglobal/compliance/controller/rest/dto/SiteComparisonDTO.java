package org.openelisglobal.compliance.controller.rest.dto;

public class SiteComparisonDTO {

    public enum ColorBand {
        GREEN, YELLOW, RED
    }

    private String siteId;
    private String siteName;
    private double complianceRate;
    private int totalOrders;
    private int exceedances;
    private ColorBand colorBand;

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String v) {
        siteId = v;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String v) {
        siteName = v;
    }

    public double getComplianceRate() {
        return complianceRate;
    }

    public void setComplianceRate(double v) {
        complianceRate = v;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int v) {
        totalOrders = v;
    }

    public int getExceedances() {
        return exceedances;
    }

    public void setExceedances(int v) {
        exceedances = v;
    }

    public ColorBand getColorBand() {
        return colorBand;
    }

    public void setColorBand(ColorBand v) {
        colorBand = v;
    }
}
