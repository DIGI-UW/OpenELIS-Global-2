package org.openelisglobal.reports.dataexport.form;

import java.util.List;

public record SavedReportPage(List<SavedReportView> reports, boolean hasMore, int page) {
    public SavedReportPage {
        reports = List.copyOf(reports);
    }
}
