package org.openelisglobal.compliance.controller.rest.dto;

import java.util.List;

public class SiteParameterTrendDTO {
    private String siteId;
    private String siteName;
    private List<ParameterSeriesDTO> parameters;

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

    public List<ParameterSeriesDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterSeriesDTO> v) {
        parameters = v;
    }
}
