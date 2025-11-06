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
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class HPVColumnBuilder extends CIStudyColumnBuilder {
    private DateType dateType;
    private List<Test> hpvTestList;

    public HPVColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    public HPVColumnBuilder(DateRange dateRange, String projectStr, DateType dateType) {
        super(dateRange, projectStr);
        this.dateType = dateType;
        hpvTestList = SpringContext.getBean(TestService.class).getActiveTestsByPanel("HPV HR");
    }

    @Override
    protected void defineBasicColumns() {
        add("accession_number", "LABNO", NONE);
        add("status_id", "SAMPLE_STATUS", SAMPLE_STATUS);
        add("external_id", "ID_PATIENT", NONE);
        add("project_id", "STUDY", PROJECT);
        add("received_date", "DRCPT", DATE_TIME); // reception date
        add("collection_date", "DINTV", DATE_TIME); // interview date
        add("organization_code", "CODE_SITE", NONE);
        add("organization_name", "NAME_SITE", NONE);
        add("datim_org_code", "CODE_SITE_DATIM", NONE);
        add("datim_org_name", "NAME_SITE_DATIM", NONE);
        add("birth_date", "BIRTH_DATE", DATE);
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
        hpvTestList = SpringContext.getBean(TestService.class).getActiveTestsByPanel("HPV HR");
        for (Test test : hpvTestList) {
            add(test.getName(), test.getName(), Strategy.DICT);
        }
    }

    protected void appendResultCrosstab(Date lowDate, Date highDate) {
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
        SQLConstant listName = SQLConstant.RESULT;
        query.append(", \n\n ( SELECT si.samp_id, si.id AS sampleItem_id, si.sort_order AS sampleItemNo, " + listName
                + ".* " + " FROM sample_item AS si LEFT JOIN \n ");

        // Begin cross tab / pivot table
        query.append(" crosstab( \n" + " 'SELECT si.id, t.description, r.value \n"
                + " FROM clinlims.result AS r, clinlims.analysis AS a, clinlims.sample_item AS si,"
                + " clinlims.sample AS s, clinlims.test AS t, clinlims.test_result AS tr \n" + " WHERE \n"
                + " s.id = si.samp_id AND " + dateColumn + " >= date(''" + formatDateForDatabaseSql(lowDate)
                + "'')  AND " + dateColumn + " <= date(''" + formatDateForDatabaseSql(highDate) + " '') "
                + "\n AND s.id = si.samp_id " + "\n AND si.id = a.sampitem_id "
                // sql injection safe as user cannot overwrite validStatusId in database
                + ((validStatusId == null) ? "" : " AND a.status_id = " + validStatusId) + "\n"
                + " AND a.id = r.analysis_id AND r.test_result_id = tr.id  AND tr.test_id = t.id      " + " \n"
                + " ORDER BY 1, 2 \n"
                + " ', 'SELECT t.description FROM test t JOIN panel_item pi ON t.id = pi.test_id  JOIN"
                + " panel p ON p.id =pi.panel_id AND p.name = ''HPV HR'' where t.is_active = ''Y''"
                + " ORDER BY 1' ) ");
        query.append("\n as " + listName + " ( " // inner use of the list name
                + "\"si_id\" numeric(10) ");
        for (Test col : hpvTestList) {
            query.append("\n, " + prepareColumnName(col.getName()) + " varchar(200) ");
        }
        query.append(" ) \n");
        // left join all sample Items from the right sample range to the results table.
        query.append("\n ON si.id = " + listName + ".si_id " // the inner use a few lines above
                + "\n ORDER BY si.samp_id, si.id " + "\n) AS " + listName + "\n "); // outer re-use the list name to
        // name this sparse matrix of
    }

    /**
     * @return the SQL for (nearly) one big row for each sample in the date range
     *         for the particular project.
     */
    protected String buildWhereSamplePatienOrgSQL(Date lowDate, Date highDate) {
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
        String WHERE_SAMPLE_PATIENT_ORG = " WHERE " + "\n pat.id = sh.patient_id " + "\n AND sh.samp_id = s.id "
                + "\n AND " + dateColumn + " >= '" + formatDateForDatabaseSql(lowDate) + "'" + "\n AND " + dateColumn
                + " <= '" + formatDateForDatabaseSql(highDate) + "'" + "\n " + "\n AND sq.requester_type_id = rq.id "
                + "\n AND sq.sample_id= s.id " + "\n AND pat.person_id = per.id " + "\n AND o.id = sq.requester_id   ";
        return WHERE_SAMPLE_PATIENT_ORG;
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

        if (ObjectUtils.isEmpty(hpvTestList)) {
            hpvTestList = SpringContext.getBean(TestService.class).getActiveTestsByPanel("HPV HR");
        }

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
