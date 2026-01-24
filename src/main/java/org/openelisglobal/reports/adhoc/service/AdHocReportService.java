package org.openelisglobal.reports.adhoc.service;

import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.AdHocReportResultDTO;

public interface AdHocReportService {

    AdHocReportResultDTO executeReport(AdHocReportDefinitionDTO definition);

    byte[] generatePdfReport(AdHocReportDefinitionDTO definition);

    boolean validateReportDefinition(AdHocReportDefinitionDTO definition);
}
