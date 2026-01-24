package org.openelisglobal.reports.adhoc.dto;

import java.util.ArrayList;
import java.util.List;

public class AdHocReportDefinitionDTO {

    private List<String> selectedFields = new ArrayList<>();
    private List<FilterCriteriaDTO> filters = new ArrayList<>();
    private String sortBy;
    private SortOrder sortOrder = SortOrder.ASC;
    private Integer limit;
    private Integer offset;
    private String reportTitle;

    public AdHocReportDefinitionDTO() {
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public void setSelectedFields(List<String> selectedFields) {
        this.selectedFields = selectedFields != null ? selectedFields : new ArrayList<>();
    }

    public List<FilterCriteriaDTO> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterCriteriaDTO> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public boolean hasPatientFields() {
        return selectedFields.stream().anyMatch(f -> f.startsWith("patient."));
    }

    public boolean hasSampleFields() {
        return selectedFields.stream().anyMatch(f -> f.startsWith("sample."));
    }

    public List<String> getPatientFields() {
        return selectedFields.stream().filter(f -> f.startsWith("patient.")).toList();
    }

    public List<String> getSampleFields() {
        return selectedFields.stream().filter(f -> f.startsWith("sample.")).toList();
    }

    public enum SortOrder {
        ASC, DESC
    }

    @Override
    public String toString() {
        return "AdHocReportDefinitionDTO{selectedFields=" + selectedFields + ", filters=" + filters + ", sortBy='"
                + sortBy + "', sortOrder=" + sortOrder + ", limit=" + limit + ", offset=" + offset + ", reportTitle='"
                + reportTitle + "'}";
    }
}
