package org.openelisglobal.program.service;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import net.sf.jasperreports.engine.JRException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.reports.action.implementation.IReportCreator;
import org.openelisglobal.reports.action.implementation.PatientPathologyReport;
import org.openelisglobal.reports.action.implementation.ReportImplementationFactory;
import org.openelisglobal.reports.form.ReportForm;
import org.openpdf.text.DocumentException;
import org.springframework.stereotype.Component;

/**
 * Runs the existing patient pathology report for a case, as the report print
 * endpoint runs it, and answers the bytes instead of streaming them.
 */
@Component
public class PatientPathologyReportRenderer implements PathologyReportRenderer {

    private static final String REPORT_NAME = "PatientPathologyReport";

    @Override
    @SuppressWarnings("unchecked")
    public byte[] render(Integer pathologySampleId, String sysUserId, ReportPaths paths, boolean amended) {
        IReportCreator reportCreator = ReportImplementationFactory.getReportCreator(REPORT_NAME);
        if (!(reportCreator instanceof PatientPathologyReport)) {
            throw new LIMSRuntimeException("no report creator is registered for " + REPORT_NAME);
        }
        ((PatientPathologyReport) reportCreator).setAmended(amended);
        reportCreator.setSystemUserId(sysUserId);
        reportCreator.setRequestedReport(REPORT_NAME);
        ReportForm form = new ReportForm();
        form.setReport(REPORT_NAME);
        form.setProgramSampleId(String.valueOf(pathologySampleId));
        reportCreator.initializeReport(form);
        reportCreator.setReportPath(paths.reportPath());
        HashMap<String, String> parameterMap = (HashMap<String, String>) reportCreator.getReportParameters();
        parameterMap.put("SUBREPORT_DIR", paths.reportPath());
        parameterMap.put("imagesPath", paths.imagesPath());
        try {
            return reportCreator.runReport();
        } catch (IOException | SQLException | JRException | DocumentException | ParseException e) {
            throw new LIMSRuntimeException("the pathology report could not be rendered", e);
        }
    }
}
