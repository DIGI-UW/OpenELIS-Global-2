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
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.valueholder.TestReflex;

public class TestReflexCD4Provider extends BaseQueryProvider {

    protected ResultService resultService = SpringContext.getBean(ResultService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);

    private String CD4_TEST_ID;
    private String GB_TEST_ID;
    private String LYMPH_TEST_ID;

    private final String VALUE_SEPERATOR = ",";
    protected AjaxServlet ajaxServlet = null;

    private static int MAX_DOUBLE_LENGTH = 20;

    private TestReflexCD4Provider() {
        Test test = testService.getTestByLocalizedName("CD4 percentage count", Locale.ENGLISH);
        if (test != null) {
            CD4_TEST_ID = test.getId();
        }
        test = testService.getTestByLocalizedName("Lymph %", Locale.ENGLISH);
        if (test != null) {
            LYMPH_TEST_ID = test.getId();
        }
        test = testService.getTestByLocalizedName("GB", Locale.ENGLISH);
        if (test != null) {
            GB_TEST_ID = test.getId();
        }
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String resultIds = request.getParameter("results");
        String values = request.getParameter("values");
        String childRow = request.getParameter("childRow");
        StringBuilder xml = new StringBuilder(childRow.length() + MAX_DOUBLE_LENGTH);
        String result = VALID;

        if (initialConditionsSatisfied(resultIds, values, childRow)) {
            result = INVALID;
            xml.append("Internal error, please contact Admin and file bug report");
        } else {
            result = createTestReflexXML(resultIds, values, childRow, xml);
        }
        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    private boolean initialConditionsSatisfied(String resultIds, String values, String childRow) {
        return GenericValidator.isBlankOrNull(resultIds) || GenericValidator.isBlankOrNull(values)
                || GenericValidator.isBlankOrNull(childRow) || GenericValidator.isBlankOrNull(CD4_TEST_ID)
                || GenericValidator.isBlankOrNull(GB_TEST_ID) || GenericValidator.isBlankOrNull(LYMPH_TEST_ID);
    }

    private String createTestReflexXML(String resultIds, String values, String childRow, StringBuilder xml) {
        String[] resultIdSeries = resultIds.split(VALUE_SEPERATOR);
        String CD4Result = "";
        String GBResult = "";
        String LymphResult = "";

        if (resultIdSeries.length > 0) {
            String[] valueSeries = values.split(VALUE_SEPERATOR);

            for (int i = 0; i < resultIdSeries.length; i++) {
                Result result = resultService.getResultById(resultIdSeries[i]);

                if (result != null) {
                    String testId = result.getAnalysis().getTest().getId();

                    if (CD4_TEST_ID.equals(testId)) {
                        CD4Result = valueSeries[i];
                    } else if (LYMPH_TEST_ID.equals(testId)) {
                        LymphResult = valueSeries[i];
                    } else if (GB_TEST_ID.equals(testId)) {
                        GBResult = valueSeries[i];
                    }
                }
            }

            CD4Result = fetchIfNeeded(CD4_TEST_ID, CD4Result, resultIdSeries[0]);
            LymphResult = fetchIfNeeded(LYMPH_TEST_ID, LymphResult, resultIdSeries[0]);
            GBResult = fetchIfNeeded(GB_TEST_ID, GBResult, resultIdSeries[0]);

            double result = 0;
            try {
                result = Double.parseDouble(CD4Result) * Double.parseDouble(GBResult) * Double.parseDouble(LymphResult)
                        * 0.1;
                result = Math.rint(result);
            } catch (NumberFormatException e) {
                return INVALID;
            }

            XMLUtil.appendKeyValue("childRow", childRow, xml);
            XMLUtil.appendKeyValue("value", String.valueOf(result), xml);
            return VALID;
        }

        return INVALID;
    }

    private String fetchIfNeeded(String testId, String currentResult, String validResultId) {
        if (!GenericValidator.isBlankOrNull(currentResult)) {
            return currentResult;
        }

        Result result = resultService.getResultById(validResultId);
        Sample sample = result.getAnalysis().getSampleItem().getSample();

        List<Result> resultList = resultService.getResultsForTestAndSample(sample.getId(), testId);

        if (resultList.isEmpty()) {
            return null;
        } else {
            return resultList.get(0).getValue();
        }
    }

    @SuppressWarnings("unused")
    private void createChoiceElement(TestReflex testReflex, StringBuilder xml) {

        XMLUtil.appendKeyValue("conclusionText", testReflex.getActionScriptlet().getScriptletName(), xml);
        XMLUtil.appendKeyValue("conclusionId", testReflex.getActionScriptletId(), xml);
        XMLUtil.appendKeyValue("testText", TestServiceImpl.getUserLocalizedTestName(testReflex.getAddedTest()), xml);
        XMLUtil.appendKeyValue("testId", testReflex.getAddedTestId(), xml);
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
