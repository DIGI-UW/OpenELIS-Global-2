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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.validation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.CustomDateValidator;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.owasp.encoder.Encode;

public class DateValidationProvider extends BaseValidationProvider {

    public DateValidationProvider() {
        super();
    }

    public DateValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // get id from request
        String dateString = request.getParameter("date");
        String relative = request.getParameter("relativeToNow");
        String formField = request.getParameter("field");

        String result = INVALID;

        if (DateUtil.yearSpecified(dateString)) {
            dateString = DateUtil.normalizeAmbiguousDate(dateString);
            Date date = getDate(dateString);
            result = validateDate(date, relative);
        }
        ajaxServlet.sendData(Encode.forXmlContent(formField), result, request, response);
    }

    public Date getDate(String date) {
        return CustomDateValidator.getInstance().getDate(date);
    }

    public String validateDate(Date date, String relative) {
        return CustomDateValidator.getInstance().validateDate(date, DateRelation.valueOf(relative.toUpperCase()));
    }
}
