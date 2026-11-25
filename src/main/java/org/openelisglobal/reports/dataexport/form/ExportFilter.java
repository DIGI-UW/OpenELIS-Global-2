package org.openelisglobal.reports.dataexport.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

/**
 * The period and scope of one export. {@code dateBasis} names the date the
 * period applies to where a report type offers a choice (OGC-1655); absent, the
 * report type's own date is used, so requests made before the choice existed
 * keep their meaning.
 */
public record ExportFilter(String dateFrom, String dateTo, List<String> labSectionIds, List<String> testIds,
        List<String> resultStatuses, String dateBasis) {
    @JsonCreator
    public ExportFilter {
        labSectionIds = labSectionIds == null ? List.of() : List.copyOf(labSectionIds);
        testIds = testIds == null ? List.of() : List.copyOf(testIds);
        resultStatuses = resultStatuses == null ? List.of() : List.copyOf(resultStatuses);
        dateBasis = dateBasis == null || dateBasis.isBlank() ? null : dateBasis;
    }

    public ExportFilter(String dateFrom, String dateTo, List<String> labSectionIds, List<String> testIds,
            List<String> resultStatuses) {
        this(dateFrom, dateTo, labSectionIds, testIds, resultStatuses, null);
    }
}
