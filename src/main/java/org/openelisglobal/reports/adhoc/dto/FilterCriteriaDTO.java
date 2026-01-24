package org.openelisglobal.reports.adhoc.dto;

import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO.FilterOperator;

public class FilterCriteriaDTO {

    private String fieldId;
    private FilterOperator operator;
    private String value;
    private String valueTo;

    public FilterCriteriaDTO() {
    }

    public FilterCriteriaDTO(String fieldId, FilterOperator operator, String value) {
        this.fieldId = fieldId;
        this.operator = operator;
        this.value = value;
    }

    public FilterCriteriaDTO(String fieldId, FilterOperator operator, String value, String valueTo) {
        this.fieldId = fieldId;
        this.operator = operator;
        this.value = value;
        this.valueTo = valueTo;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public FilterOperator getOperator() {
        return operator;
    }

    public void setOperator(FilterOperator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueTo() {
        return valueTo;
    }

    public void setValueTo(String valueTo) {
        this.valueTo = valueTo;
    }

    public boolean isRangeFilter() {
        return operator == FilterOperator.BETWEEN;
    }

    public boolean isNullCheck() {
        return operator == FilterOperator.IS_NULL || operator == FilterOperator.IS_NOT_NULL;
    }

    @Override
    public String toString() {
        return "FilterCriteriaDTO{fieldId='" + fieldId + "', operator=" + operator + ", value='" + value
                + "', valueTo='" + valueTo + "'}";
    }
}
