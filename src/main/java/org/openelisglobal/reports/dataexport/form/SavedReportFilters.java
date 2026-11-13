package org.openelisglobal.reports.dataexport.form;

import java.util.List;

public record SavedReportFilters(List<String> labSectionIds, List<String> testIds, List<String> resultStatuses) {
    public SavedReportFilters {
        labSectionIds = labSectionIds == null ? List.of() : List.copyOf(labSectionIds);
        testIds = testIds == null ? List.of() : List.copyOf(testIds);
        resultStatuses = resultStatuses == null ? List.of() : List.copyOf(resultStatuses);
    }
}
