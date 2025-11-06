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

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.LOG;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;

import java.sql.Date;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class ForCIDashboardColumnBuilder extends CIColumnBuilder {

    public ForCIDashboardColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        add("Viral Load", "Viral Load", NONE);
        add("Viral Load", "Viral Load log", LOG);
        add("type_of_sample_name", "Type_of_sample", NONE);
        add("started_date", "STARTED_DATE", DATE_TIME);
        add("completed_date", "COMPLETED_DATE", DATE_TIME);
        add("released_date", "RELEASED_DATE", DATE_TIME);
        // add("patient_oe_id" ,"PATIENT_OE_ID", NONE);

        /*
         * add("hivStatus" , "STATVIH", DICT_RAW ); add("nameOfDoctor" , "NOMMED", NONE
         * ); add("nameOfSampler" , "NOMPRELEV", NONE ); /* add("arvTreatmentInitDate" ,
         * "ARV_INIT_DATE", NONE ); add("arvTreatmentRegime" , "ARVREG" );
         */

        /*
         * add("currentARVTreatmentINNs1", "CURRENT1",NONE );
         * add("currentARVTreatmentINNs2", "CURRENT2",NONE );
         * add("currentARVTreatmentINNs3", "CURRENT3",NONE );
         * add("currentARVTreatmentINNs4", "CURRENT4",NONE );
         */

        // add("currentARVTreatment" , "CURRENT_ART", DICT_RAW );
        // add("vlReasonForRequest" , "VL_REASON" );
        /*
         * add("currentARVTreatment" , "CURRENT_ART" ); add("vlReasonForRequest" ,
         * "VL_REASON", DICT_RAW ); add("vlOtherReasonForRequest" , "REASON_OTHER", NONE
         * );
         *
         * add("initcd4Count" , "INITCD4_COUNT", NONE ); add("initcd4Percent" ,
         * "INITCD4_PERCENT", NONE ); add("initcd4Date" , "INITCD4_DATE", NONE );
         *
         * add("demandcd4Count" , "DEMANDCD4_COUNT", NONE ); add("demandcd4Percent" ,
         * "DEMANDCD4_PERCENT", NONE ); add("demandcd4Date" , "DEMANDCD4_DATE", NONE );
         */

        // add("vlBenefit" , "PRIOR_VL_BENEFIT",NONE );
        /*
         * add("vlBenefit" , "PRIOR_VL_BENEFIT"); add("vlPregnancy" , "VL_PREGNANCY");
         * add("vlSuckle" , "VL_SUCKLE"); add("priorVLLab" , "PRIOR_VL_Lab",NONE );
         * add("priorVLValue" , "PRIOR_VL_Value",NONE ); add("priorVLDate" ,
         * "PRIOR_VL_Date",NONE );
         */
        add("report_name", "report_name", NONE);
        add("report_generation_time", "report_generation_time", DATE_TIME);
        // addAllResultsColumns();

    }

    /**
     * @return the SQL for (nearly) one big row for each sample in the date range
     *         for the particular project.
     */
    @Override
    public void makeSQL() {
        Test test = SpringContext.getBean(TestService.class).getActiveTestsByName("Viral Load").get(0);
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the following list and in
        // the WHERE clause at the bottom
        query.append("\n" + ", pat.id AS patient_oe_id,"
                + " a.started_date,a.completed_date,a.released_date,a.printed_date, r.value as \"Viral"
                + " Load\", a.type_of_sample_name, dt.name as report_name,dt.report_generation_time ");

        // ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION
                + ", clinlims.sample_item as si, clinlims.analysis as a, clinlims.result as r,"
                + " clinlims.document_track as dt ");

        // all observation history values
        // appendObservationHistoryCrosstab(lowDatePostgres, highDatePostgres);
        // current ARV treatments
        // appendRepeatingObservation("currentARVTreatmentINNs", 4, lowDatePostgres,
        // highDatePostgres, "dt.report_generation_time");
        // result
        // appendResultCrosstab(lowDatePostgres, highDatePostgres );

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(" WHERE " + "\n dt.report_generation_time >= date('" + formatDateForDatabaseSql(lowDate) + "')"
                + "\n AND dt.report_generation_time <= date('" + formatDateForDatabaseSql(highDate) + "')"
                + "\n AND dt.name = 'patientVL1'" + "\n AND a.test_id =" + test.getId() + "\n AND dt.row_id=s.id"
                + "\n AND si.samp_id=s.id" + "\n AND a.sampitem_id = si.id" + "\n AND a.id=r.analysis_id"
                + "\n AND s.id=sh.samp_id" + "\n AND sh.patient_id=pat.id" + "\n AND pat.person_id = per.id"
                + "\n AND s.id=so.samp_id" + "\n AND so.org_id=o.id" + "\n AND s.id = sp.samp_id"
                // + "\n AND s.id=demo.s_id"
                // + "\n AND s.id = currentARVTreatmentINNs.samp_id"

                + "\n ORDER BY s.accession_number;");
        /////////
        // no don't insert another crosstab or table here, go up before the main WHERE
        ///////// clause
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        ///////// query.toString());
        return;
    }
}
