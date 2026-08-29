package org.openelisglobal.compliance.controller.rest.dto;

import java.util.List;

public class SiteSeriesDTO {
    private String siteId;
    private String siteName;
    private String siteCode;
    private List<MonthDataPointDTO> dataPoints;

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

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String v) {
        siteCode = v;
    }

    public List<MonthDataPointDTO> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<MonthDataPointDTO> v) {
        dataPoints = v;
    }
}
