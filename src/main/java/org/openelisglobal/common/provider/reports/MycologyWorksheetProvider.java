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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.common.provider.reports;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperRunManager;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.springframework.validation.Errors;

/**
 * @author benzd1
 */
public class MycologyWorksheetProvider extends BaseReportsProvider {

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.provider.reports.BaseReportsProvider#
     * processRequest(java.util.Map, jakarta.servlet.http.HttpServletRequest,
     * jakarta.servlet.http.HttpServletResponse) bugzilla 2274: added boolean
     * successful
     */
    @Override
    public boolean processRequest(Map parameters, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        ServletContext context = session.getServletContext();
        File reportFile = new File(context.getRealPath("/WEB-INF/reports/specimen_list.jasper"));

        ServletOutputStream servletOutputStream = response.getOutputStream();

        byte[] bytes = null;
        Connection conn = null;

        Errors errors = new BaseErrors();

        try {

            InitialContext ic = new InitialContext();
            DataSource nativeDS = (DataSource) ic
                    .lookup(ConfigurationProperties.getInstance().getPropertyValue("default.datasource").toString());
            conn = nativeDS.getConnection();

            // get yesterday's date as date received

            // Put the current system date in a Calendar object.
            GregorianCalendar gc = new GregorianCalendar();
            // Subtract one day from the object.
            gc.add(Calendar.DATE, -1);
            // Get a Date object based on the Calendar object.
            Date dayAgo = gc.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String dateAsText = sdf.format(dayAgo);

            // convert string date to java.util.Date by going through java.sql.Date
            // bugzilla 2274 - date conversion fixed
            String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
            java.sql.Date sDate = DateUtil.convertStringDateToSqlDate(dateAsText, locale);
            java.util.Date date = new java.util.Date(sDate.getTime());

            parameters.put("Param_Received_Date", date);

            bytes = JasperRunManager.runReportToPdf(reportFile.getPath(), parameters, conn);

            response.setContentType("application/pdf");
            response.setContentLength(bytes.length);

            servletOutputStream.write(bytes, 0, bytes.length);
            servletOutputStream.flush();
            servletOutputStream.close();
        } catch (JRException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            // display stack trace in the browser
            StringWriter stringWriter = new StringWriter();
            response.setContentType("text/plain");
            response.getOutputStream().print(stringWriter.toString());
            errors.reject("errors.jasperreport.general");
        } catch (SQLException | NamingException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            errors.reject("errors.jasperreport.general");
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                // bugzilla 2154
                LogEvent.logError(e);
            }
        }

        if (errors.hasErrors()) {
            request.setAttribute(Constants.REQUEST_ERRORS, errors);
            return false;
        } else {
            return true;
        }
    }
}
