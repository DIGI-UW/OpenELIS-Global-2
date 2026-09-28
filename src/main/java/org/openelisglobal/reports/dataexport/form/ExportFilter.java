package org.openelisglobal.reports.dataexport.form;

import java.util.List;

public record ExportFilter(String dateFrom, String dateTo, List<String> labSectionIds, List<String> testIds,
        List<String> resultStatuses) {
    public ExportFilter {
        labSectionIds = labSectionIds == null ? List.of() : List.copyOf(labSectionIds);
        testIds = testIds == null ? List.of() : List.copyOf(testIds);
        resultStatuses = resultStatuses == null ? List.of() : List.copyOf(resultStatuses);
    }
}
