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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.spring.util.SpringContext;
import org.owasp.encoder.Encode;

public class PendingAnalysisForTestProvider extends BaseQueryProvider {

    private static final List<Integer> NOT_STARTED;
    private static final List<Integer> TECH_REJECT;
    private static final List<Integer> BIO_REJECT;
    private static final List<Integer> NOT_VALIDATED;

    protected AjaxServlet ajaxServlet = null;
    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    static {
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        NOT_STARTED = new ArrayList<>();
        NOT_STARTED.add(Integer.parseInt(statusService.getStatusID(StatusService.AnalysisStatus.NotStarted)));

        TECH_REJECT = new ArrayList<>();
        TECH_REJECT.add(Integer.parseInt(statusService.getStatusID(StatusService.AnalysisStatus.TechnicalRejected)));

        BIO_REJECT = new ArrayList<>();
        BIO_REJECT.add(Integer.parseInt(statusService.getStatusID(StatusService.AnalysisStatus.BiologistRejected)));

        NOT_VALIDATED = new ArrayList<>();
        NOT_VALIDATED
                .add(Integer.parseInt(statusService.getStatusID(StatusService.AnalysisStatus.TechnicalAcceptance)));
    }

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
            jResult = createJsonGroupedAnalysis(testId, jsonResult);
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
    private String createJsonGroupedAnalysis(String testId, JSONObject jsonResult) throws IllegalStateException {
        createPendingList(testId, jsonResult, "notStarted", NOT_STARTED);
        createPendingList(testId, jsonResult, "technicianRejection", TECH_REJECT);
        createPendingList(testId, jsonResult, "biologistRejection", BIO_REJECT);
        createPendingList(testId, jsonResult, "notValidated", NOT_VALIDATED);
        return VALID;
    }

    private void createPendingList(String testId, JSONObject jsonResult, String tag, List<Integer> statusList) {
        List<Analysis> notStartedAnalysis = analysisService.getAllAnalysisByTestAndStatus(testId, statusList);

        JSONArray analysisArray = new JSONArray();

        for (Analysis analysis : notStartedAnalysis) {
            JSONObject analysisObject = new JSONObject();
            analysisObject.put("labNo", analysis.getSampleItem().getSample().getAccessionNumber());
            analysisObject.put("id", analysis.getId());
            analysisArray.add(analysisObject);
        }
        jsonResult.put(tag, analysisArray);
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
