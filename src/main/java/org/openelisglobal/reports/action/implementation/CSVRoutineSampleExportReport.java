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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import javax.xml.ws.Response;
import net.sf.jasperreports.engine.JRException;
import org.openelisglobal.reports.action.implementation.reportBeans.RoutineColumnBuilder;

public abstract class CSVRoutineSampleExportReport extends CSVRoutineExportReport {

    protected String lowDateStr;
    protected String highDateStr;
    protected String selectedLabUnit;
    protected List<Object> reportItems;
    protected int iReportItem = -1;

    protected RoutineColumnBuilder csvRoutineColumnBuilder;
    protected DateRange dateRange;

    @Override
    public String getResponseHeaderName() {
        return "Content-Disposition";
    }

    @Override
    public String getResponseHeaderContent() {
        return "attachment;filename=" + getReportFileName() + ".csv";
    }

    /**
     * Do everything necessary for to generate a CSV text file.
     *
     * @param reportDefinitionName full path to the definition for the report.
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws JRException
     * @throws SQLException
     * @throws IllegalStateException
     * @throws ParseException
     * @see org.openelisglobal.reports.action.implementation.IReportCreator#runReport(java.lang.String,
     *      Response)
     */
    @Override
    public byte[] runReport() throws UnsupportedEncodingException, IOException, IllegalStateException, SQLException,
            JRException, ParseException {
        if (errorFound) {
            return super.runReport();
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(100000);
        buffer.write(csvRoutineColumnBuilder.getColumnNamesLine().getBytes("windows-1252"));

        writeResultsToBuffer(buffer);
        csvRoutineColumnBuilder.closeResultSet();

        return buffer.toByteArray();
    }

    protected void writeResultsToBuffer(ByteArrayOutputStream buffer)
            throws IOException, UnsupportedEncodingException, SQLException, ParseException {
        while (csvRoutineColumnBuilder.next()) {
            buffer.write(csvRoutineColumnBuilder.nextLine().getBytes("windows-1252"));
        }
    }
}
