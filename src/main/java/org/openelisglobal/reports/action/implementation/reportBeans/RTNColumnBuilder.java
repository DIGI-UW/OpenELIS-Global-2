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

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT_PLUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT_RAW;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;

/**
 * @author pahill (pahill@uw.edu)
 * @since May 18, 2011
 */
public class RTNColumnBuilder extends CIColumnBuilder {
    private DateType dateType;

    /**
     * @param dateRange
     * @param projectStr
     */
    public RTNColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIColumnBuilder#defineAllReportColumns()
     */
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        add("hivStatus", "STATVIH", DICT_RAW);
        add("nationality", "PAYNAIS", DICT_PLUS);
        add("hospitalPatient", "HOSP", DICT);
        for (String disease : getRTNDiseaseQuestionNames()) {
            add(disease, disease);
        }
        for (String exam : getRTNExamQuestionNames()) {
            add(exam, exam);
        }
        addAllResultsColumns();
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIColumnBuilder#makeSQL()
     */
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
            dateColumn = "dt.report_generation_time ";
        default:
            break;
        }
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // more cross tabulation of other columns goes where
        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);

        // FROM clause for ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history from expressions
        appendObservationHistoryCrosstab(lowDate, highDate, dateColumn);

        appendResultCrosstab(lowDate, highDate, dateColumn);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDate, highDate)
                // insert joining of any other crosstab here.
                + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id " + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }

    public String[] getRTNDiseaseQuestionNames() {
        List<Dictionary> rtnList = ObservationHistoryList.RTN_DISEASES.getList();
        List<String> priorDiseaseList = new ArrayList<>();
        for (Dictionary dictionary : rtnList) {
            priorDiseaseList.add(dictionary.getLocalAbbreviation());
        }
        return priorDiseaseList.toArray(new String[8]);
    }

    public String[] getRTNExamQuestionNames() {
        List<Dictionary> rtnList = ObservationHistoryList.RTN_EXAM_DISEASES.getList();
        List<String> priorDiseaseList = new ArrayList<>();
        for (Dictionary dictionary : rtnList) {
            priorDiseaseList.add(dictionary.getLocalAbbreviation());
        }
        return priorDiseaseList.toArray(new String[8]);
    }
}
