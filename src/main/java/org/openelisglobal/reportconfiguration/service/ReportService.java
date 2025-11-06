package org.openelisglobal.reportconfiguration.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reportconfiguration.form.ReportConfigurationForm;
import org.openelisglobal.reportconfiguration.valueholder.Report;

public interface ReportService extends BaseObjectService<Report, String> {

    List<Report> getReports();

    boolean updateReport(ReportConfigurationForm form, String currentUserId);

    boolean createNewReport(ReportConfigurationForm form, String currentUserId);
}
