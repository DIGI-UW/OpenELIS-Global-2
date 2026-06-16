package org.openelisglobal.compliance.controller.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public class ParameterSeriesDTO {
    private String parameterCode;
    private String displayName;
    private String units;
    private BigDecimal threshold;
    private String thresholdType;
    private boolean isDescriptive;
    private List<ParameterDataPointDTO> dataPoints;

    public String getParameterCode() {
        return parameterCode;
    }

    public void setParameterCode(String v) {
        parameterCode = v;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String v) {
        displayName = v;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String v) {
        units = v;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal v) {
        threshold = v;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String v) {
        thresholdType = v;
    }

    public boolean isDescriptive() {
        return isDescriptive;
    }

    public void setDescriptive(boolean v) {
        isDescriptive = v;
    }

    public List<ParameterDataPointDTO> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<ParameterDataPointDTO> v) {
        dataPoints = v;
    }
}
