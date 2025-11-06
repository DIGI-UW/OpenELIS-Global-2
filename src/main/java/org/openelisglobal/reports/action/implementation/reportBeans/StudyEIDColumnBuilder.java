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

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.ANALYSIS_STATUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.TEST_RESULT;

import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

/**
 * @author pahill (pahill@uw.edu)
 * @since May 16, 2011
 */
public class StudyEIDColumnBuilder extends CIStudyColumnBuilder {
    private DateType dateType;

    /**
     * @param dateRange
     * @param projectStr
     */
    public StudyEIDColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    public StudyEIDColumnBuilder(DateRange dateRange, String projectStr, DateType dateType) {
        super(dateRange, projectStr);
        this.dateType = dateType;
    }

    /** This is the order we want them in the CSV file. */
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();

        add("DNA PCR", "DNA PCR", TEST_RESULT);
        add("analysis_status_id", "ANALYSIS_STATUS", ANALYSIS_STATUS);
        add("started_date", "STARTED_DATE", DATE_TIME);
        add("completed_date", "COMPLETED_DATE", DATE_TIME);
        add("released_date", "RELEASED_DATE", DATE_TIME);
        // add("patient_oe_id" ,"PATIENT_OE_ID", NONE);// a means to check unknown
        // patient with id=1

        add("nameOfSampler", "NAMEPREV", NONE);
        add("nameOfRequestor", "NAMEMED", NONE);
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

        add("report_name", "REPORT_NAME", NONE);
        add("report_generation_time", "PRINTED_DATE", DATE_TIME);
        add("report_lastupdated", "LAST_REPORT_UPDATE", DATE_TIME);
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
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the SELECT column list and
        // in the WHERE clause at the bottom
        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);
        // more cross tabulation of other columns goes where

        // ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history from expressions
        appendObservationHistoryCrosstab(dateRange.getLowDate(), dateRange.getHighDate(), dateColumn);
        appendResultCrosstab(dateRange.getLowDate(), dateRange.getHighDate(), dateColumn);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(postgresDateFormat.format(dateRange.getLowDate()),
                postgresDateFormat.format(dateRange.getHighDate()), dateColumn)
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
        // String validStatusId =
        // StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
        Test test = SpringContext.getBean(TestService.class).getActiveTestByName("DNA PCR").get(0);
        query = new StringBuilder();
        String lowDatePostgres = postgresDateFormat.format(dateRange.getLowDate());
        String highDatePostgres = postgresDateFormat.format(dateRange.getHighDate());
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the following list and in
        // the WHERE clause at the bottom
        query.append("\n" + ", pat.id AS patient_oe_id,"
                + " a.started_date,a.completed_date,a.released_date,a.printed_date, a.status_id as"
                + " analysis_status_id, r.value as \"DNA PCR\", demo.*, dt.name as report_name,"
                + " first_dt.report_generation_time, dt.lastupdated as report_lastupdated ");

        // ordinary lab (sample and patient) tables
        /*
         * query.append( FROM_SAMPLE_PATIENT_ORGANIZATION +
         * ", clinlims.sample_item as si, clinlims.analysis as a, clinlims.document_track as dt, clinlims.result as r "
         * );
         */

        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        /*
         * // Take account sample without result query.append(
         * "LEFT JOIN  result as r on r.analysis_id = a.id ");
         */

        // all observation history values
        appendObservationHistoryCrosstab(dateRange.getLowDate(), dateRange.getHighDate(), dateColumn);
        // current ARV treatments
        // appendRepeatingObservation("currentARVTreatmentINNs", 4, lowDatePostgres,
        // highDatePostgres);
        // result
        // appendResultCrosstab(lowDatePostgres, highDatePostgres );

        query.append(",  clinlims.analysis as a \n");
        // -------------------------------------

        query.append(" LEFT JOIN  clinlims.result as r on r.analysis_id = a.id \n"
                + " LEFT JOIN  clinlims.sample_item as si on si.id = a.sampitem_id \n"
                + " LEFT JOIN  clinlims.sample as s on s.id = si.samp_id \n"
                + " LEFT JOIN  (select max(id)as id, row_id \n" + "       from clinlims.document_track \n"
                + "           group by (row_id ) \n" + "           order by row_id DESC) as dtr on dtr.row_id=s.id \n"
                + " LEFT JOIN clinlims.document_track as dt on dtr.id=dt.id \n"
                + " LEFT JOIN  (select min(id)as id, row_id from clinlims.document_track \n"
                + " group by (row_id ) order by row_id ASC) as first_dtr on first_dtr.row_id=s.id \n"
                + " LEFT JOIN clinlims.document_track as first_dt on first_dtr.id=first_dt.id \n");

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(" WHERE " + "\n a.test_id =" + test.getId()
        // + "\n AND dt.row_id=s.id"
        // + "\n AND a.id=r.analysis_id"
                + "\n AND a.sampitem_id = si.id" + "\n AND s.id = si.samp_id" + "\n AND s.id=sh.samp_id"
                + "\n AND sh.patient_id=pat.id" + "\n AND pat.person_id = per.id" + "\n AND s.id=so.samp_id"
                + "\n AND so.org_id=o.id" + "\n AND s.id = sp.samp_id" + "\n AND s.id=demo.s_id" + "\n AND "
                + dateColumn + "  >= date('" + lowDatePostgres + "')" + "\n AND " + dateColumn + "  <= date('"
                + highDatePostgres + "')" + "\n ORDER BY s.accession_number;");
        /////////
        // no don't insert another crosstab or table here, go up before the main WHERE
        ///////// clause
        return;
    }
}
