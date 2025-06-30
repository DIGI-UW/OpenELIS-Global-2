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
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONObject;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.owasp.encoder.Encode;

public class TestNamesProvider extends BaseQueryProvider {

    protected AjaxServlet ajaxServlet = null;

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String testId = request.getParameter("testId");

        String jResult;
        JSONObject jsonResult = new JSONObject();
        String jString;

        if (GenericValidator.isBlankOrNull(testId)) {
            jResult = INVALID;
            jString = "Internal error, please contact Admin and file bug report";
        } else {
            jResult = createJsonTestNames(testId, jsonResult);
            StringWriter out = new StringWriter();
            try {
                jsonResult.writeJSONString(out);
                jString = out.toString();
            } catch (IOException e) {
                LogEvent.logDebug(e);
                jResult = INVALID;
                jString = "Internal error, please contact Admin and file bug report";
            } catch (IllegalStateException e) {
                LogEvent.logDebug(e);
                jResult = INVALID;
                jString = "Internal error, please contact Admin and file bug report";
            }
        }
        ajaxServlet.sendData(Encode.forXmlContent(jString), Encode.forXmlContent(jResult), request, response);
    }

    @SuppressWarnings("unchecked")
    private String createJsonTestNames(String testId, JSONObject jsonResult) throws IllegalStateException {

        if (GenericValidator.isBlankOrNull(testId)) {
            throw new IllegalStateException("TestNamesProvider testId was blank.  It must have a value");
        }

        Test test = SpringContext.getBean(TestService.class).get(testId);
        if (test != null) {
            Localization nameLocalization = test.getLocalizedTestName();
            Localization reportNameLocalization = test.getLocalizedReportingName();

            JSONObject nameObject = new JSONObject();
            addAllLocalizations(nameObject, nameLocalization);
            // nameObject.put("english", nameLocalization.getEnglish());
            // nameObject.put("french", nameLocalization.getFrench());
            jsonResult.put("name", nameObject);

            JSONObject reportingNameObject = new JSONObject();
            addAllLocalizations(reportingNameObject, reportNameLocalization);
            // reportingNameObject.put("english", reportNameLocalization.getEnglish());
            // reportingNameObject.put("french", reportNameLocalization.getFrench());
            jsonResult.put("reportingName", reportingNameObject);

            return VALID;
        }

        return INVALID;
    }

    @SuppressWarnings("unchecked")
    private void addAllLocalizations(JSONObject jsonObject, Localization localization) {
        for (Locale locale : localization.getLocalesWithValue()) {
            jsonObject.put(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase(),
                    localization.getLocalizedValue(locale));
        }
    }

    @Override
    public void setServlet(AjaxServlet as) {
        ajaxServlet = as;
    }

    @Override
    public AjaxServlet getServlet() {
        return ajaxServlet;
    }
}
