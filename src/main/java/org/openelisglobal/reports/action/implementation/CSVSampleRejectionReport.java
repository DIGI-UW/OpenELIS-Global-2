package org.openelisglobal.reports.action.implementation;

import java.sql.SQLException;
import org.jfree.util.Log;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVSampleRejectionColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;

public class CSVSampleRejectionReport extends CSVSampleExportReport implements IReportParameterSetter, IReportCreator {

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(getReportNameForParameterPage());
            form.setUseLowerDateRange(Boolean.TRUE);
            form.setUseUpperDateRange(Boolean.TRUE);
        } catch (RuntimeException e) {
            Log.error("Error in ExportProjectByDate.setRequestParemeters: ", e);
        }
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("openreports.mgt.rejection");
    }

    @Override
    protected String reportFileName() {
        return "SampleRejection";
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        dateRange = new DateRange(lowDateStr, highDateStr);

        createReportParameters();
        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }

        createReportItems();
    }

    /** creating the list for generation to the report */
    private void createReportItems() {
        try {
            csvColumnBuilder = new CSVSampleRejectionColumnBuilder(dateRange);
            csvColumnBuilder.buildDataSource();
        } catch (SQLException e) {
            Log.error("Error in " + this.getClass().getSimpleName() + ".createReportItems: ", e);
            add1LineErrorMessage("report.error.message.general.error");
        }
    }

    private boolean validateSubmitParameters() {
        return dateRange.validateHighLowDate("report.error.message.date.received.missing");
    }
}
