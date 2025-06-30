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

import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import net.sf.jasperreports.engine.JRException;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.reports.form.ReportForm;

public interface IReportCreator {
    String INCOMPLETE_PARAMS = "Incompleate parameters";
    String INVALID_PARAMS = "Invalid parameters";
    String SUCCESS = IActionConstants.FWD_SUCCESS;

    void initializeReport(ReportForm form);

    String getResponseHeaderName();

    String getContentType();

    String getResponseHeaderContent();

    HashMap<String, ?> getReportParameters() throws IllegalStateException;

    byte[] runReport() throws UnsupportedEncodingException, IOException, SQLException, IllegalStateException,
            JRException, DocumentException, ParseException;

    void setReportPath(String path);

    void setRequestedReport(String report);

    List<String> getReportedOrders();

    void setSystemUserId(String id);
}
