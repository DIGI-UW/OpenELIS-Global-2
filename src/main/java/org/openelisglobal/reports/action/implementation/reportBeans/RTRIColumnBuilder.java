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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

// import static
// org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.SAMPLE_STATUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.ANALYSIS_STATUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class RTRIColumnBuilder extends CIStudyColumnBuilder {
    private DateType dateType;

    public RTRIColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    public RTRIColumnBuilder(DateRange dateRange, String projectStr, DateType dateType) {
        super(dateRange, projectStr);
        this.dateType = dateType;
    }

    // @Override
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        add("Asante HIV-1 Rapid Recency", "RECENCY TEST RESULT", Strategy.DICT);
        add("type_of_sample_name", "TYPE_OF_SAMPLE", NONE);
        add("analysis_status_id", "ANALYSIS_STATUS", ANALYSIS_STATUS);
        add("started_date", "STARTED_DATE", DATE_TIME);
        add("completed_date", "COMPLETED_DATE", DATE_TIME);
        add("released_date", "RELEASED_DATE", DATE_TIME);

        // add("hivStatus", "STATVIH", DICT_RAW);
        add("nameOfDoctor", "NAMEMED", NONE);
        add("nameOfSampler", "NAMEPRELEV", NONE);

        add("vlPregnancy", "VL_PREGNANCY");
        add("vlSuckle", "VL_SUCKLE");

        // addAllResultsColumns();

    }

    /**
     * @return the SQL for (nearly) one big row for each sample in the date range
     *         for the particular project.
     */
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
            dateColumn = "a.released_date ";
        default:
            break;
        }
        // String validStatusId =
        // StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
        // String validStatusId2 =
        // StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);

        List<Test> tests = SpringContext.getBean(TestService.class).getActiveTestByName("Asante HIV-1 Rapid Recency");
        String ids = tests.stream().map(e -> {
            return e.getId();
        }).collect(Collectors.joining(","));
        query = new StringBuilder();
        String lowDatePostgres = postgresDateFormat.format(dateRange.getLowDate());
        String highDatePostgres = postgresDateFormat.format(dateRange.getHighDate());
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the following list and in
        // the WHERE clause at the bottom
        query.append("\n" + ", a.started_date,a.completed_date,a.released_date, a.status_id as"
                + " analysis_status_id, r.value as \"Asante HIV-1 Rapid" + " Recency\",a.type_of_sample_name, demo.* ");

        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // --------------------------
        // all observation history values
        appendObservationHistoryCrosstab(dateRange.getLowDate(), dateRange.getHighDate(), dateColumn);

        // result
        // appendResultCrosstab(dateRange.getLowDate(), dateRange.getHighDate() );
        query.append(",  clinlims.analysis as a \n");
        // -------------------------------------

        query.append(" LEFT JOIN  clinlims.result as r on r.analysis_id = a.id \n"
                + " LEFT JOIN  clinlims.sample_item as si on si.id = a.sampitem_id \n"
                + " LEFT JOIN  clinlims.sample as s on s.id = si.samp_id \n");

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(" WHERE a.test_id in (" + ids + ")\n AND a.sampitem_id = si.id"
                + "\n AND s.id = si.samp_id AND s.id=sh.samp_id  AND sh.patient_id=pat.id"
                + "\n AND pat.person_id = per.id AND s.id=so.samp_id AND so.org_id=o.id"
                + "\n AND s.id = sp.samp_id AND s.id=demo.s_id" + "\n " + "\n AND " + dateColumn + " >= date('"
                + lowDatePostgres + "')" + "\n AND " + dateColumn + " <= date('" + highDatePostgres + "')"

                // --------------
                + "\n ORDER BY s.accession_number;");
        /////////
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }
}
