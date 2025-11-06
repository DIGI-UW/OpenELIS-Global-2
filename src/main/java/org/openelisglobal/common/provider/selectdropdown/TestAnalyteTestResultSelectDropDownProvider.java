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
package org.openelisglobal.common.provider.selectdropdown;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresult.valueholder.TestResultComparator;

/**
 * An example servlet that responds to an ajax:autocomplete tag action. This
 * servlet would be referenced by the baseUrl attribute of the JSP tag.
 *
 * <p>
 * This servlet should generate XML in the following format:
 * <code><![CDATA[<?xml version="1.0"?>
 * <list>
 *   <item value="Item1">First Item</item>
 *   <item value="Item2">Second Item</item>
 *   <item value="Item3">Third Item</item>
 * </list>]]></code>
 *
 * @author Darren L. Spurgeon
 */
public class TestAnalyteTestResultSelectDropDownProvider extends BaseSelectDropDownProvider {

    protected DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    protected TestAnalyteService testAnalyteService = SpringContext.getBean(TestAnalyteService.class);
    protected TestResultService testResultService = SpringContext.getBean(TestResultService.class);

    /**
     * @see org.ajaxtags.demo.servlet.BaseAjaxServlet#getXmlContent(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public List processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // System.out
        // .println("I am in TestAnalyteTestResultSelectDropDownProvider ");

        String testAnalyteId = request.getParameter("testAnalyteId");

        List listOfTestResults = new ArrayList();

        TestAnalyte testAnalyte = new TestAnalyte();

        if (!StringUtil.isNullorNill(testAnalyteId)) {
            testAnalyte.setId(testAnalyteId);
            testAnalyteService.getData(testAnalyte);
            listOfTestResults = testResultService.getTestResultsByTestAndResultGroup(testAnalyte);
        }

        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Returning
        // from
        // Running getTestResultsByTestAndResultGr
        // ");
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "size ofo
        // list "
        // + listOfTestResults.size());
        if (listOfTestResults != null && listOfTestResults.size() > 0) {
            for (int i = 0; i < listOfTestResults.size(); i++) {
                // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "one elem
                // " +
                // listOfTestResults.get(i));
            }
        }
        // for testResults load the value field with dict entry if needed
        List list = new ArrayList();
        if (listOfTestResults != null) {
            for (int i = 0; i < listOfTestResults.size(); i++) {
                TestResult tr = new TestResult();
                tr = (TestResult) listOfTestResults.get(i);
                if (tr.getTestResultType()
                        .equals(ConfigurationProperties.getInstance().getPropertyValue("dictionaryType"))) {
                    // get from dictionary
                    Dictionary dictionary = new Dictionary();
                    dictionary.setId(tr.getValue());
                    dictionaryService.getData(dictionary);
                    // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "setting
                    // dictEntry "
                    // + dictionary.getDictEntry());
                    // bugzilla 1847: use dictEntryDisplayValue
                    tr.setValue(dictionary.getDictEntryDisplayValue());
                }
                list.add(tr);
            }
        }

        Collections.sort(list, TestResultComparator.VALUE_COMPARATOR);

        return list;
    }
}
