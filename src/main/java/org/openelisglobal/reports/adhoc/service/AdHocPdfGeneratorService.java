package org.openelisglobal.reports.adhoc.service;

import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.AdHocReportResultDTO;

public interface AdHocPdfGeneratorService {

    byte[] generatePdf(AdHocReportDefinitionDTO definition, AdHocReportResultDTO reportData);
}
