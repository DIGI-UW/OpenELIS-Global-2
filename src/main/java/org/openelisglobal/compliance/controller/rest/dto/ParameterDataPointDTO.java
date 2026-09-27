package org.openelisglobal.compliance.controller.rest.dto;

public class ParameterDataPointDTO {
    private String month;
    private Double avgValue;
    private Double maxValue;
    private int exceedanceCount;

    public String getMonth() {
        return month;
    }

    public void setMonth(String v) {
        month = v;
    }

    public Double getAvgValue() {
        return avgValue;
    }

    public void setAvgValue(Double v) {
        avgValue = v;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double v) {
        maxValue = v;
    }

    public int getExceedanceCount() {
        return exceedanceCount;
    }

    public void setExceedanceCount(int v) {
        exceedanceCount = v;
    }
}
