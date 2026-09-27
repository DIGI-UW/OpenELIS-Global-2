package org.openelisglobal.reports.dataexport.form;

import java.util.List;

public record SavedReportDefinition(int schemaVersion, String reportType, String layout, List<String> selectedVariables,
        SavedReportFilters filters) {
    public SavedReportDefinition {
        selectedVariables = selectedVariables == null ? List.of() : List.copyOf(selectedVariables);
        filters = filters == null ? new SavedReportFilters(null, null, null) : filters;
    }
}
