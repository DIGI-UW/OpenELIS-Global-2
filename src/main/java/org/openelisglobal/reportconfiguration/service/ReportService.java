package org.openelisglobal.reportconfiguration.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reportconfiguration.form.ReportConfigurationForm;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReportService extends BaseObjectService<Report, String> {

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    List<Report> getReports();

    @PreAuthorize("hasAuthority('PRIV_REPORT_CONFIGURE')")
    boolean updateReport(ReportConfigurationForm form, String currentUserId);

    @PreAuthorize("hasAuthority('PRIV_REPORT_CONFIGURE')")
    boolean createNewReport(ReportConfigurationForm form, String currentUserId);
}
