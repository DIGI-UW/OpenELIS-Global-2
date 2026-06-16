package org.openelisglobal.compliance.controller.rest.dto;

import java.util.List;

public class DashboardTrendDTO {
    private List<String> months;
    private List<SiteSeriesDTO> series;

    public List<String> getMonths() {
        return months;
    }

    public void setMonths(List<String> v) {
        months = v;
    }

    public List<SiteSeriesDTO> getSeries() {
        return series;
    }

    public void setSeries(List<SiteSeriesDTO> v) {
        series = v;
    }
}
