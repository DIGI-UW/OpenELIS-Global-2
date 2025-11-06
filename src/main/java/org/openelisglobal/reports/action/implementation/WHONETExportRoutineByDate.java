/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.sql.SQLException;
import org.jfree.util.Log;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class WHONETExportRoutineByDate extends WHONETCSVRoutineSampleExportReport implements IReportParameterSetter {
    protected final ProjectService projectService = SpringContext.getBean(ProjectService.class);

    @Override
    protected String reportFileName() {
        return "WHONETExportRoutineByDate";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(getReportNameForParameterPage());
            form.setUseLowerDateRange(Boolean.TRUE);
            form.setUseUpperDateRange(Boolean.TRUE);
        } catch (RuntimeException e) {
            Log.error("Error in ExportRoutineByDate.setRequestParemeters: ", e);
        }
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.project.export") + " "
                + MessageUtil.getContextualMessage("sample.collectionDate");
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

    /** check everything */
    // -----------------------------------
    private boolean validateSubmitParameters() {
        return dateRange.validateHighLowDate("report.error.message.date.received.missing");
    }

    /** creating the list for generation to the report */
    private void createReportItems() {
        try {
            WHONETcsvRoutineColumnBuilder = getColumnBuilder();
            WHONETcsvRoutineColumnBuilder.buildDataSource();
        } catch (SQLException e) {
            Log.error("Error in " + this.getClass().getSimpleName() + ".createReportItems: ", e);
            add1LineErrorMessage("report.error.message.general.error");
        }
    }

    private WHONETCSVRoutineColumnBuilder getColumnBuilder() {
        return new WHONETCSVRoutineColumnBuilder(dateRange);
    }

}