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
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.service.WHONetReportService;
import org.openelisglobal.spring.util.SpringContext;

/**
 * @author pahill (pahill@uw.edu)
 * @since Mar 18, 2011
 */
public class WHONETCSVRoutineColumnBuilder {

    public static class WHONetRow {
        private String nationalId;
        private String firstName;
        private String lastName;
        private String sex;
        private String birthdate;
        private String enteredDate;
        private String labNo;
        private String collectionDate;
        private String sampleType;
        private String antibiotic;
        private String organism;
        private String result;
        private String method;

        private String delimeter = "\t";

        public WHONetRow(String nationalId, String firstName, String lastName, String sex, String birthdate,
                String enteredDate, String labNo, String collectionDate, String sampleType, String antibiotic,
                String organism, String result, String method) {
            this.nationalId = nationalId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.sex = sex;
            this.birthdate = birthdate;
            this.enteredDate = enteredDate;
            this.labNo = labNo;
            this.collectionDate = collectionDate;
            this.sampleType = sampleType;
            this.antibiotic = antibiotic;
            this.organism = organism;
            this.result = result;
            this.method = method;
        }

        public String getRow() {
            List<String> rowValues = Arrays.asList(nationalId, firstName, lastName, sex, birthdate, enteredDate, labNo,
                    collectionDate, sampleType, antibiotic, organism, result, method);
            return String.join(delimeter, rowValues);
        }
    }

    private List<WHONetRow> rows;
    private int index = -1;

    private String eol = System.getProperty("line.separator");
    private DateRange dateRange;

    /**
     * @param dateRange
     * @param projectStr
     */
    public WHONETCSVRoutineColumnBuilder(DateRange dateRange) {
        this.dateRange = dateRange;
    }

    public void searchForWHONetResults() {
        WHONetReportService reportService = SpringContext.getBean(WHONetReportService.class);
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        rows = reportService.getWHONetRows(lowDate, highDate);
        return;
    }

    public void buildDataSource() throws SQLException {
        searchForWHONetResults();
    }

    /**
     * Useful for the 1st line of a CSV files. This produces a completely escaped
     * for MSExcel comma separated list of columns.
     *
     * @return one string with all names.
     */
    public String getColumnNamesLine() {
        return new StringBuilder().append(
                new WHONetRow("NATIONAL ID", "FIRST NAME", "LAST NAME", "SEX", "BIRTHDATE", "DATE ENTERED", "LABNO",
                        "DATE COLLECTED", "SPECIMEN TYPE", "ANTIBIOTIC", "ORGANISM", "RESULT", "METHOD").getRow())
                .append(eol).toString();
    }

    /**
     * @return @
     * @throws ParseException
     * @throws SQLException
     */
    public String nextLine() throws SQLException, ParseException {
        return new StringBuilder().append(rows.get(index).getRow()).append(eol).toString();
    }

    public boolean next() throws SQLException {
        return ++index < rows.size();
    }
}
