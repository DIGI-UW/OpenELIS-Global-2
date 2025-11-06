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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.owasp.encoder.Encode;

public class TestResultLimitsProvider extends BaseQueryProvider {

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
            jResult = createJsonTestResultLimits(testId, jsonResult);
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
    private String createJsonTestResultLimits(String testId, JSONObject jsonResult) throws IllegalStateException {

        if (GenericValidator.isBlankOrNull(testId)) {
            throw new IllegalStateException("TestResultLimitsProvider testId was blank.  It must have a value");
        }

        Test test = SpringContext.getBean(TestService.class).get(testId);
        if (test != null) {
            List<ResultLimit> resultLimits = SpringContext.getBean(ResultLimitService.class).getResultLimits(testId);
            resultLimits.sort(Comparator.comparing(ResultLimit::getMinAge));
            Collections.sort(resultLimits, new Comparator<ResultLimit>() {
                @Override
                public int compare(ResultLimit e1, ResultLimit e2) {
                    if (e1.getMinAge() == e2.getMinAge()) {
                        return e2.getGender().compareTo(e1.getGender());
                    }
                    return Double.compare(e1.getMinAge(), e2.getMinAge());
                }
            });
            JSONArray limitsArray = new JSONArray();
            for (ResultLimit resultLimit : resultLimits) {

                JSONObject resultLimitObject = new JSONObject();

                resultLimitObject.put("id", resultLimit.getId());
                resultLimitObject.put("gender", resultLimit.getGender());
                resultLimitObject.put("minAge", resultLimit.getMinAge());
                resultLimitObject.put("maxAge", resultLimit.getMaxAge());
                resultLimitObject.put("lowNormal", resultLimit.getLowNormal());
                resultLimitObject.put("highNormal", resultLimit.getHighNormal());
                resultLimitObject.put("lowValid", resultLimit.getLowValid());
                resultLimitObject.put("highValid", resultLimit.getHighValid());
                resultLimitObject.put("highCritical", resultLimit.getHighCritical());
                resultLimitObject.put("lowCritical", resultLimit.getLowCritical());
                limitsArray.add(resultLimitObject);
            }

            jsonResult.put("resultLimits", limitsArray);

            return VALID;
        }

        return INVALID;
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
