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
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;

/**
 * @author pahill (pahill@uw.edu)
 * @since May 18, 2011
 */
public abstract class ARVColumnBuilder extends CIColumnBuilder {
    private DateType dateType;

    /**
     * @param dateRange
     * @param projectStr
     */
    public ARVColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    /** This is the order we want them in the CSV file. */
    @Override
    protected abstract void defineAllReportColumns();

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
            dateColumn = "dt.report_generation_time ";
        default:
            break;
        }

        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the following list and in
        // the WHERE clause at the bottom
        query.append("\n, demo.*, priorDiseaseOther.*, priorARVTreatmentINNs.*"
                + "\n, futureARVTreatmentINNs.*, currentDiseaseOther.* "
                + "\n, arvTreatmentAdvEffType.*, arvTreatmentAdvEffGrd.* "
                + "\n, cotrimoxazoleTreatAdvEffType.*, cotrimoxazoleTreatAdvEffGrd.* " + "\n, result.* " + "\n ");

        // ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history values
        appendObservationHistoryCrosstab(lowDate, highDate, dateColumn);

        // prior diseases
        appendOtherDiseaseCrosstab(lowDate, highDate, SQLConstant.PRIOR_DISEASES, SQLConstant.PRIOR_DISEASE_OTHER);

        // prior treatments
        appendRepeatingObservation(SQLConstant.PRIOR_ARV_TREATMENT_INNS, 4, lowDate, highDate);

        appendRepeatingObservation(SQLConstant.FUTURE_ARV_TREATMENT_INNS, 4, lowDate, highDate);

        appendOtherDiseaseCrosstab(lowDate, highDate, SQLConstant.CURRENT_DISEASES, SQLConstant.CURRENT_DISEASE_OTHER);
        appendRepeatingObservation(SQLConstant.ARV_TREATMENT_ADV_EFF_GRD, 4, lowDate, highDate);
        appendRepeatingObservation(SQLConstant.ARV_TREATMENT_ADV_EFF_TYPE, 4, lowDate, highDate);
        appendRepeatingObservation(SQLConstant.COTRIMOXAZOLE_TREAT_ADV_EFF_GRD, 4, lowDate, highDate);
        appendRepeatingObservation(SQLConstant.COTRIMOXAZOLE_TREAT_ADV_EFF_TYPE, 4, lowDate, highDate);
        appendResultCrosstab(lowDate, highDate, dateColumn);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDate, highDate) + "\n AND s.id = demo.samp_id "
                + "\n AND s.id = priorDiseaseOther.samp_id " + "\n AND s.id = priorARVTreatmentINNs.samp_id "
                + "\n AND s.id = futureARVTreatmentINNs.samp_id " + "\n AND s.id = currentDiseaseOther.samp_id "
                + "\n AND s.id = arvTreatmentAdvEffGrd.samp_id " + "\n AND s.id = arvTreatmentAdvEffType.samp_id "
                + "\n AND s.id = cotrimoxazoleTreatAdvEffType.samp_id "
                + "\n AND s.id = cotrimoxazoleTreatAdvEffGrd.samp_id " + "\n AND s.id = result.samp_id "
                + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }
}
