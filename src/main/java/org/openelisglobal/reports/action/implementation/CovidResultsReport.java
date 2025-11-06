package org.openelisglobal.reports.action.implementation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.CovidResultsBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.CovidResultsBuilderImpl.CovidReportType;
import org.openelisglobal.reports.action.implementation.reportBeans.CovidResultsCSVBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.CovidResultsJSONBuilder;
import org.openelisglobal.reports.form.ReportForm;

public class CovidResultsReport extends Report implements IReportParameterSetter, IReportCreator {

    private CovidResultsBuilder covidDataBuilder;
    protected String lowDateStr;
    protected String highDateStr;
    protected DateRange dateRange;
    protected CovidReportType reportType;

    @Override
    protected String reportFileName() {
        return "CovidResults";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(getReportNameForParameterPage());
            form.setUseLowerDateRange(Boolean.TRUE);
            form.setUseUpperDateRange(Boolean.TRUE);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "setRequestParameters",
                    "Runtime exception occured while setting params");
        }
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.project.export") + " "
                + MessageUtil.getContextualMessage("sample.collectionDate");
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        dateRange = new DateRange(lowDateStr, highDateStr);
        reportType = CovidReportType.valueOf(form.getType());

        createReportParameters();
        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }
        createReportItems();
    }

    /** check everything */
    private boolean validateSubmitParameters() {
        return dateRange.validateHighLowDate("report.error.message.date.received.missing");
    }

    /** creating the list for generation to the report */
    private void createReportItems() {
        covidDataBuilder = getDataBuilder();
        covidDataBuilder.buildDataSource();
    }

    private CovidResultsBuilder getDataBuilder() {
        switch (reportType) {
        case JSON:
            return new CovidResultsJSONBuilder(dateRange);
        case CSV:
            return new CovidResultsCSVBuilder(dateRange);
        }
        throw new IllegalStateException("type must be 'CSV' or 'JSON'");
    }

    @Override
    public byte[] runReport() throws UnsupportedEncodingException, IOException, IllegalStateException, SQLException,
            JRException, ParseException {
        return covidDataBuilder.getDataSourceAsByteArray();
    }

    @Override
    public String getResponseHeaderName() {
        return "Content-Disposition";
    }

    @Override
    public String getContentType() {
        if (errorFound) {
            return super.getContentType();
        } else {
            return "text/plain; charset=UTF-8";
        }
    }

    @Override
    public String getResponseHeaderContent() {
        switch (reportType) {
        case JSON:
            return "attachment;filename=" + getReportFileName() + ".json";
        case CSV:
            return "attachment;filename=" + getReportFileName() + ".csv";
        default:
            throw new IllegalStateException("type must be 'CSV' or 'JSON'");
        }
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }
        if (errorFound) {
            return new JRBeanCollectionDataSource(errorMsgs);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
