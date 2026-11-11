package org.openelisglobal.compliance.controller.rest.dto;

public class SiteComparisonDTO {

    public enum ColorBand {
        GREEN, YELLOW, RED
    }

    private String siteId;
    private String siteName;
    private Double complianceRate;
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

    /**
     * Null when none of the site's results in the period has a threshold to be
     * judged against: the site has no compliance rate to compare.
     */
    public Double getComplianceRate() {
        return complianceRate;
    }

    public void setComplianceRate(Double v) {
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
