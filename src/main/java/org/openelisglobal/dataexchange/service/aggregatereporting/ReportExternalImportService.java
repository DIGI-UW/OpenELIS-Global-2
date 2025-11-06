package org.openelisglobal.dataexchange.service.aggregatereporting;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalImport;

public interface ReportExternalImportService extends BaseObjectService<ReportExternalImport, String> {
    List<ReportExternalImport> getReportsInDateRangeSortedForSite(Timestamp beginning, Timestamp end, String site);

    List<ReportExternalImport> getReportsInDateRangeSorted(Timestamp lower, Timestamp upper);

    ReportExternalImport getReportByEventDateSiteType(ReportExternalImport importReport);

    List<String> getUniqueSites();

    void updateReports(List<ReportExternalImport> insertableImportReports,
            List<ReportExternalImport> updatableImportReports);
}
