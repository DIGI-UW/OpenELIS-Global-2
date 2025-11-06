/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.TEST_RESULT;

import java.sql.Date;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

/**
 * @author pahill (pahill@uw.edu)
 * @since May 16, 2011
 */
public class EIDColumnBuilder extends CIColumnBuilder {
    private DateType dateType;

    TestService testService = SpringContext.getBean(TestService.class);

    /**
     * @param dateRange
     * @param projectStr
     */
    public EIDColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    /** This is the order we want them in the CSV file. */
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();

        add("DNA PCR", "DNA PCR", TEST_RESULT);
        add("started_date", "STARTED_DATE", DATE_TIME);
        add("completed_date", "COMPLETED_DATE", DATE_TIME);
        add("released_date", "RELEASED_DATE", DATE_TIME);
        // add("patient_oe_id" ,"PATIENT_OE_ID", NONE);// a means to check unknown
        // patient with id=1

        add("nameOfSampler", "NOMPREV", NONE);
        add("nameOfRequestor", "NOMMED", NONE);
        add("whichPCR", "whichPCR");
        add("reasonForSecondPCRTest", "reasonForSecondPCRTest");
        add("eidInfantPTME", "eidInfantPTME");
        add("eidTypeOfClinic", "eidTypeOfClinic", new EIDTypeOfClinicStrategy());
        add("eidInfantSymptomatic", "eidInfantSymptomatic");
        add("eidMothersHIVStatus", "eidMothersHIVStatus");
        add("eidMothersARV", "eidMothersARV");
        add("eidInfantsARV", "eidInfantsARV");
        add("eidInfantCotrimoxazole", "eidInfantCotrimoxazole");
        add("eidHowChildFed", "eidHowChildFed");
        add("eidStoppedBreastfeeding", "eidStoppedBreastfeeding");
        // addAllResultsColumns();
    }

    /**
     * @return the SQL for (nearly) one big row for each sample in the date range
     *         for the particular project.
     */
    public void makeSQL_original() { // without analysis completed date ......

        // Switch date column according to selected DateType: PK
        String dateColumn = "s.entered_date ";
        switch (dateType) {
        case ORDER_DATE:
            dateColumn = "s.entered_date ";
            break;
        case RESULT_DATE:
            dateColumn = "a.released_date ";
            break;
        case PRINT_DATE:
            dateColumn = "dt.report_generation_time ";
        default:
            break;
        }
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the SELECT column list and
        // in the WHERE clause at the bottom
        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);
        // more cross tabulation of other columns goes where

        // ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history from expressions
        appendObservationHistoryCrosstab(lowDate, highDate, dateColumn);
        appendResultCrosstab(lowDate, highDate, dateColumn);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDate, highDate)
                // insert joining of any other crosstab here.
                // insert joining of any other crosstab here.
                + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id " + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause

        return;
    }

    @Override
    public void makeSQL() {
        // Switch date column according to selected DateType: PK
        String dateColumn = "s.entered_date ";
        switch (dateType) {
        case ORDER_DATE:
            dateColumn = "s.entered_date ";
            break;
        case RESULT_DATE:
            dateColumn = "a.released_date ";
            break;
        case PRINT_DATE:
            dateColumn = "dt.report_generation_time ";
        default:
            break;
        }
        String validStatusId = SpringContext.getBean(IStatusService.class)
                .getStatusID(StatusService.AnalysisStatus.Finalized);
        Test test = testService.getActiveTestsByName("DNA PCR").get(0);
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the following list and in
        // the WHERE clause at the bottom
        query.append("\n" + ", pat.id AS patient_oe_id,"
                + " a.started_date,a.completed_date,a.released_date,a.printed_date, r.value as \"DNA"
                + " PCR\", demo.* ");

        // ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION
                + ", clinlims.sample_item as si, clinlims.analysis as a, clinlims.result as r ");

        // all observation history values
        appendObservationHistoryCrosstab(lowDate, highDate, dateColumn);
        // current ARV treatments
        // appendRepeatingObservation("currentARVTreatmentINNs", 4, lowDate,
        // highDate);
        // result
        // appendResultCrosstab(lowDate, highDate );

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(" WHERE " + "\n a.test_id =" + test.getId()
                + ((validStatusId == null) ? "" : " AND a.status_id = " + validStatusId) + "\n AND a.id=r.analysis_id"
                + "\n AND a.sampitem_id = si.id" + "\n AND s.id = si.samp_id" + "\n AND s.id=sh.samp_id"
                + "\n AND sh.patient_id=pat.id" + "\n AND pat.person_id = per.id" + "\n AND s.id=so.samp_id"
                + "\n AND so.org_id=o.id" + "\n AND s.id = sp.samp_id" + "\n AND s.id=demo.s_id"
                // + "\n AND s.id = currentARVTreatmentINNs.samp_id"
                + "\n AND s.collection_date >= date('" + formatDateForDatabaseSql(lowDate) + "')"
                + "\n AND s.collection_date <= date('" + formatDateForDatabaseSql(highDate) + "')"
                + "\n ORDER BY s.accession_number;");
        /////////
        // no don't insert another crosstab or table here, go up before the main WHERE
        ///////// clause

        return;
    }
}
