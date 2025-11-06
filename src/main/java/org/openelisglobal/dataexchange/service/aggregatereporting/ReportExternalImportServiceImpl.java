package org.openelisglobal.dataexchange.service.aggregatereporting;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dataexchange.aggregatereporting.dao.ReportExternalImportDAO;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExternalImportServiceImpl extends AuditableBaseObjectServiceImpl<ReportExternalImport, String>
        implements ReportExternalImportService {
    @Autowired
    protected ReportExternalImportDAO baseObjectDAO;

    public ReportExternalImportServiceImpl() {
        super(ReportExternalImport.class);
    }

    @Override
    protected ReportExternalImportDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalImport> getReportsInDateRangeSortedForSite(Timestamp beginning, Timestamp end,
            String site) {
        return getBaseObjectDAO().getReportsInDateRangeSortedForSite(beginning, end, site);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalImport> getReportsInDateRangeSorted(Timestamp lower, Timestamp upper) {
        return getBaseObjectDAO().getReportsInDateRangeSorted(lower, upper);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExternalImport getReportByEventDateSiteType(ReportExternalImport importReport) {
        return getBaseObjectDAO().getReportByEventDateSiteType(importReport);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUniqueSites() {
        return getBaseObjectDAO().getUniqueSites();
    }

    @Override
    @Transactional
    public void updateReports(List<ReportExternalImport> insertableImportReports,
            List<ReportExternalImport> updatableImportReports) {
        for (ReportExternalImport importReport : insertableImportReports) {
            insert(importReport);
        }

        for (ReportExternalImport importReport : updatableImportReports) {
            update(importReport);
        }
    }
}
