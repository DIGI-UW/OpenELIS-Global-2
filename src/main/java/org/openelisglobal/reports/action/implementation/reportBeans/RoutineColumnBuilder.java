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

/**
 * @author pahill (pahill@uw.edu)
 * @since May 18, 2011
 */
public class RoutineColumnBuilder extends CIRoutineColumnBuilder {

    /**
     * @param dateRange
     * @param projectStr
     */
    public RoutineColumnBuilder(DateRange dateRange) {
        super(dateRange);
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#defineAllReportColumns()
     */
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        addAllResultsColumns();
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
