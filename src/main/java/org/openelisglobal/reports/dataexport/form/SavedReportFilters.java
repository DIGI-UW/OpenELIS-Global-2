package org.openelisglobal.reports.dataexport.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

/**
 * The filters a saved report keeps. {@code dateBasis} is the date its period
 * applies to (OGC-1655); absent in reports saved before the choice existed,
 * which keep the report type's own date.
 */
public record SavedReportFilters(List<String> labSectionIds, List<String> testIds, List<String> resultStatuses,
        String dateBasis) {
    @JsonCreator
    public SavedReportFilters {
        labSectionIds = labSectionIds == null ? List.of() : List.copyOf(labSectionIds);
        testIds = testIds == null ? List.of() : List.copyOf(testIds);
        resultStatuses = resultStatuses == null ? List.of() : List.copyOf(resultStatuses);
        dateBasis = dateBasis == null || dateBasis.isBlank() ? null : dateBasis;
    }

    public SavedReportFilters(List<String> labSectionIds, List<String> testIds, List<String> resultStatuses) {
        this(labSectionIds, testIds, resultStatuses, null);
    }
}
