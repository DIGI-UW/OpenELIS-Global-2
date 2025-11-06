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

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.SQLConstant;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;

/**
 * @author pahill (pahill@uw.edu)
 * @since May 18, 2011
 */
public class TBColumnBuilder extends RoutineColumnBuilder {

    protected static final String FROM_SAMPLE_PATIENT_ORGANIZATION = " FROM sample as s, patient as pat, person as per, sample_human as sh, sample_organization"
            + " so, organization AS o \n" + " ";

    /**
     * @param dateRange
     * @param projectStr
     */
    public TBColumnBuilder(DateRange dateRange) {
        super(dateRange);
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#defineAllReportColumns()
     */
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        add("tborderreason", "ORDER_REASON", Strategy.DICT);
        add("tbanalysismethod", "ANALYSIS_METHOD", Strategy.DICT);
        add("tbdiagnosticreason", "DIAGNOSTIC_REASON", Strategy.DICT);
        add("tbfollowupreason", "FOLLOW_UP_REASON", Strategy.DICT);
        add("tbfollowupreasonperiodline1", "FOLLOW_UP_REASON_LINE1", Strategy.NONE);
        add("tbfollowupreasonperiodline2", "FOLLOW_UP_REASON_LINE2", Strategy.NONE);
        add("tbsampleaspects", "SAMPLE_ASPECT", Strategy.DICT);
        // add("report_generation_time", "PRINTED_DATE", DATE_TIME);
        // add("report_lastupdated", "LAST_REPORT_UPDATE", DATE_TIME);
        addAllResultsColumns();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void defineAllTestsAndResults() {
        if (allTests == null) {
            allTests = testService.getTbTest();
        }
        if (testResultsByTestName == null) {
            testResultsByTestName = new HashMap<>();
            List<TestResult> allTestResults = testResultService.getAllTestResults();
            for (TestResult testResult : allTestResults) {
                String key = TestServiceImpl.getLocalizedTestNameWithType(testResult.getTest());
                testResultsByTestName.put(key, testResult);
            }
        }
    }

    @Override
    protected void appendResultCrosstab(java.sql.Date lowDate, java.sql.Date highDate) {
        // A list of analytes which should not show up in the regular results,
        // String excludeAnalytes = getExcludedAnalytesSet();
        SQLConstant listName = SQLConstant.RESULT;
        query.append(", \n\n ( SELECT si.samp_id, si.id AS sampleItem_id, si.sort_order AS sampleItemNo, " + listName
                + ".* " + " FROM sample_item AS si JOIN \n ");

        // Begin cross tab / pivot table
        query.append(" crosstab( \n" + " 'SELECT si.id, t.description, replace(replace(replace(replace(r.value ,E''\\n"
                + "'', '' ''), E''\\t'', '' ''), E''\\r" + "'', '' ''),'','',''.'') \n"
                + " FROM clinlims.analysis AS a join clinlims.test AS t on a.test_id = t.id  \n"
                + "  JOIN test_section ts ON t.test_section_id = ts.id \n"
                + "  join clinlims.test_result AS tr on t.id = tr.test_id  \n"
                + " join clinlims.sample_item AS si on si.id = a.sampitem_id \n"
                + " join clinlims.sample AS s on s.id = si.samp_id  \n"
                + " left join clinlims.result AS r on a.id = r.analysis_id  \n"
                + " left join sample_projects sp on si.samp_id = sp.samp_id \n" + "\n"
                + " WHERE sp.id IS NULL AND ts.name = ''TB'' AND s.entered_date >= date(''"
                + formatDateForDatabaseSql(lowDate) + "'')  AND s.entered_date <= date(''"
                + formatDateForDatabaseSql(highDate) + " '') " + "\n "
                // sql injection safe as user cannot overwrite validStatusId in database
                // + ((validStatusId == null) ? "" : " AND a.status_id = " + validStatusId)
                // + (( excludeAnalytes == null)?"":
                // " AND r.analyte_id NOT IN ( " + excludeAnalytes) + ")"
                // + " AND a.test_id = t.id "
                + "\n" + " ORDER BY 1, 2 \n"
                + " ', 'SELECT t.description FROM test t JOIN test_section ts ON t.test_section_id ="
                + " ts.id where t.is_active = ''Y'' AND ts.name = ''TB'' ORDER BY 1' ) ");
        // end of cross tab

        // Name the test pivot table columns . We'll name them all after the
        // resource name, because some tests have fancy characters in them and
        // somewhere
        // between iReport, Java and postgres a complex name (e.g. one including
        // a beta, " HCG Quant") get messed up and isn't found.
        query.append("\n as " + listName + " ( " // inner use of the list name
                + "\"si_id\" numeric(10) ");
        for (Test col : allTests) {
            String testName = TestServiceImpl.getLocalizedTestNameWithType(col);
            query.append("\n, " + prepareColumnName(testName) + " varchar(200) ");
        }
        query.append(" ) \n");
        // left join all sample Items from the right sample range to the results table.
        query.append("\n ON si.id = " + listName + ".si_id " // the inner use a few lines above
                + "\n ORDER BY si.samp_id, si.id " + "\n) AS " + listName + "\n "); // outer re-use the list name to
        // name this sparse matrix of
        // results.
    }

    protected String buildWhereSamplePatienOrgSQL(Date lowDate, Date highDate) {
        String WHERE_SAMPLE_PATIENT_ORG = " WHERE " + "\n pat.id = sh.patient_id " + "\n AND sh.samp_id = s.id "
                + "\n AND s.entered_date >= '" + formatDateForDatabaseSql(lowDate) + "'" + "\n AND s.entered_date <= '"
                + formatDateForDatabaseSql(highDate) + "'" + "\n " + "\n AND so.samp_id= s.id "
                + "\n AND pat.person_id = per.id " + "\n AND o.id = so.org_id   "
        // + ((GenericValidator.isBlankOrNull(projectStr))?"": " AND sp.proj_id = " +
        // projectStr)
        ;
        return WHERE_SAMPLE_PATIENT_ORG;
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#makeSQL()
     */
    @Override
    public void makeSQL() {
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // more cross tabulation of other columns goes where
        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);

        // FROM clause for ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history from expressions
        appendObservationHistoryCrosstab(lowDate, highDate);

        appendResultCrosstab(lowDate, highDate);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDate, highDate)
                // insert joining of any other crosstab here.
                + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id " + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }
}
