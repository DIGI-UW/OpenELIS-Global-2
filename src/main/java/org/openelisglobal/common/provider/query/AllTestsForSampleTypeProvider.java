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
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.owasp.encoder.Encode;

public class AllTestsForSampleTypeProvider extends BaseQueryProvider {

    protected AjaxServlet ajaxServlet = null;

    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String sampleTypeId = request.getParameter("sampleTypeId");

        String jResult;
        JSONObject jsonResult = new JSONObject();
        String jString;

        if (GenericValidator.isBlankOrNull(sampleTypeId)) {
            jResult = INVALID;
            jString = "Internal error, please contact Admin and file bug report";
        } else {
            jResult = createJsonGroupedTestNames(sampleTypeId, jsonResult);
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
    private String createJsonGroupedTestNames(String sampleTypeId, JSONObject jsonResult) throws IllegalStateException {
        List<Test> tests = typeOfSampleService.getAllTestsBySampleTypeId(sampleTypeId);

        JSONArray testArray = new JSONArray();

        for (Test test : tests) {
            JSONObject testObject = new JSONObject();
            testObject.put("name", test.getLocalizedTestName().getLocalizedValue());
            testObject.put("id", test.getId());
            testObject.put("isActive", test.getIsActive());
            testArray.add(testObject);
        }
        jsonResult.put("tests", testArray);
        return VALID;
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
