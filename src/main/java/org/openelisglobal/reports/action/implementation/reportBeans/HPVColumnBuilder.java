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

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.PROJECT;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.SAMPLE_STATUS;

import java.sql.Date;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class HPVColumnBuilder extends CIStudyColumnBuilder {
    private DateType dateType;

    public HPVColumnBuilder(DateRange dateRange, String projectStr, DateType dateType) {
        super(dateRange, projectStr);
        this.dateType = dateType;
    }

    @Override
    protected void defineBasicColumns() {
        add("accession_number", "Lab No", NONE);
        add("status_id", "Sample Status", SAMPLE_STATUS);
        add("external_id", "Patient ID", NONE);
        add("project_id", "Study", PROJECT);
        add("received_date", "Date Received", DATE_TIME);
        add("collection_date", "Date Collected", DATE_TIME);
        add("organization_code", "Site Code", NONE);
        add("organization_name", "Site Name", NONE);
        add("datim_org_code", "DATIM Site Code", NONE);
        add("datim_org_name", "DATIM Site Name", NONE);
        add("birth_date", "Birth Date", DATE);
    }

    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        add("type_of_sample_name", "TYPE_OF_SAMPLE", NONE);
        // add("analysis_status_id", "ANALYSIS_STATUS", ANALYSIS_STATUS);
        add("hivStatus", "STAT_VIH", Strategy.DICT);
        add("started_date", "STARTED_DATE", DATE_TIME);
        add("completed_date", "COMPLETED_DATE", DATE_TIME);
        add("released_date", "RELEASED_DATE", DATE_TIME);
        add("hpvsamplingmethod", "SAMPLING_METHOD", Strategy.DICT);
        add("nameOfDoctor", "NAME_MED", NONE);
        // add("HPV HR", "HPV TEST RESULT", NONE);
        addAllResultsColumns();
    }

    @Override
    protected void addAllResultsColumns() {
        for (Test test : SpringContext.getBean(TestService.class).getActiveTestsByPanel("HPV HR")) {
            add(CSVColumnSql.resultColumnName(test), test.getName(), Strategy.DICT);
        }
    }

    protected static final String SELECT_SAMPLE_PATIENT_ORGANIZATION = "SELECT DISTINCT s.id as sample_id, s.accession_number, s.entered_date, s.received_date,"
            + " s.collection_date, s.status_id \n"
            + ", pat.national_id, pat.external_id, pat.birth_date, per.first_name, per.last_name," + " pat.gender \n"
            + ", o.short_name as organization_code, o.name AS organization_name, sp.proj_id as" + " project_id \n"
            + ", o.datim_org_code, o.datim_org_name,a.type_of_sample_name, a.status_id as"
            + " analysis_status_id, a.started_date,a.completed_date,a.released_date \n" + " ";

    @Override
    public void makeSQL() {
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
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();

        String lowDatePostgres = postgresDateFormat.format(dateRange.getLowDate());
        String highDatePostgres = postgresDateFormat.format(dateRange.getHighDate());

        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);

        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);

        // FROM clause for ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history from expressions
        appendObservationHistoryCrosstab(lowDate, highDate, dateColumn);

        appendResultCrosstab(lowDate, highDate, dateColumn);
        query.append(",  clinlims.analysis as a \n");

        query.append(" LEFT JOIN  clinlims.result as r on r.analysis_id = a.id \n"
                + " LEFT JOIN  clinlims.sample_item as si on si.id = a.sampitem_id \n"
                + " LEFT JOIN  clinlims.sample as s on s.id = si.samp_id \n");

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDatePostgres, highDatePostgres, dateColumn)
                // insert joining of any other crosstab here.
                + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id " + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }
}
