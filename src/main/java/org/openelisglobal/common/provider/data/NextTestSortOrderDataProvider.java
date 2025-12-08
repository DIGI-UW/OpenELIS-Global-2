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
package org.openelisglobal.common.provider.data;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.servlet.data.AjaxServlet;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.owasp.encoder.Encode;

/**
 * @author diane benz bugzilla 2443
 */
public class NextTestSortOrderDataProvider extends BaseDataProvider {

    protected TestService testService = SpringContext.getBean(TestService.class);
    protected TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);

    public NextTestSortOrderDataProvider() {
        super();
    }

    public NextTestSortOrderDataProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String testSectionId = request.getParameter("tsid");
        String formField = request.getParameter("field");
        String result = getData(testSectionId);
        ajaxServlet.sendData(Encode.forXmlContent(formField), Encode.forXmlContent(result), request, response);
    }

    // modified for efficiency bugzilla 1367
    /**
     * getData() - for NextTestSortOrderDataProvider
     *
     * @param testSectionId - String
     * @return String - data
     */
    public String getData(String testSectionId) throws LIMSRuntimeException {
        String result = INVALID;

        if (!StringUtil.isNullorNill(testSectionId)) {
            Test test = new Test();
            TestSection testSection = new TestSection();
            testSection.setId(testSectionId);
            testSectionService.getData(testSection);

            if (!StringUtil.isNullorNill(testSection.getId())) {
                test.setTestSection(testSection);

                Integer sortOrder = testService.getNextAvailableSortOrderByTestSection(test);
                if (sortOrder != null) {
                    result = sortOrder.toString();
                } else {
                    result = "1";
                }
            }
        }
        return result;
    }
}
