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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.print.PrintException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSInvalidPrinterException;
import org.openelisglobal.common.exception.LIMSPrintException;
import org.openelisglobal.common.servlet.reports.ReportsServlet;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportTest;
import org.openelisglobal.reports.valueholder.resultsreport.ResultsReportTestComparator;
import org.openelisglobal.spring.util.SpringContext;

/**
 * @author benzd1 bugzilla 2380
 */
public abstract class BasePrintProvider implements IActionConstants {

    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    protected ReportsServlet reportsServlet = null;

    /**
     * @param parameters
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws PrintException
     * @throws LIMSPrintException
     * @throws LIMSInvalidPrinterException
     */
    public abstract void processRequest(Map<?, ?> parameters, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, PrintException, LIMSPrintException, LIMSInvalidPrinterException;

    /**
     * @param rs
     */
    public void setServlet(ReportsServlet rs) {
        this.reportsServlet = rs;
    }

    /**
     * @return
     */
    public ReportsServlet getServlet() {
        return this.reportsServlet;
    }

    // bugzilla 1856
    protected List<ResultsReportTest> sortTests(List<ResultsReportTest> reportTests) {

        // find root level nodes and fill in children for each Test_TestAnalyte
        List<ResultsReportTest> rootLevelNodes = new ArrayList<>();
        for (int i = 0; i < reportTests.size(); i++) {
            ResultsReportTest reportTest = reportTests.get(i);
            String analysisId = reportTest.getAnalysis().getId();

            List<ResultsReportTest> children = new ArrayList<>();
            for (int j = 0; j < reportTests.size(); j++) {
                ResultsReportTest test = reportTests.get(j);
                if (test.getAnalysis().getParentAnalysis() != null
                        && test.getAnalysis().getParentAnalysis().getId().equals(analysisId)) {
                    children.add(test);
                }
            }
            reportTest.setChildren(children);

            if (reportTest.getAnalysis().getParentAnalysis() == null) {
                rootLevelNodes.add(reportTest);
            }
        }

        // sort rootLevelNodes
        Collections.sort(rootLevelNodes, ResultsReportTestComparator.SORT_ORDER_COMPARATOR);

        reportTests = new ArrayList<>();
        for (int i = 0; i < rootLevelNodes.size(); i++) {
            ResultsReportTest reportTest = rootLevelNodes.get(i);
            reportTests.add(reportTest);
            recursiveSort(reportTest, reportTests);
        }

        return reportTests;
    }

    // bugzilla 1856 use recursion and sort children
    private void recursiveSort(ResultsReportTest element, List<ResultsReportTest> reportTests) {
        List<ResultsReportTest> children = element.getChildren();
        // sort children
        if (children != null && children.size() > 0) {
            Collections.sort(children, ResultsReportTestComparator.SORT_ORDER_COMPARATOR);
            for (ResultsReportTest childElement : children) {
                reportTests.add(childElement);
                recursiveSort(childElement, reportTests);
            }
        }
    }

    /**
     * Utility method to simplify the lookup of MessageResource Strings in the
     * ApplicationResources.properties file for this application.
     *
     * @param request    the HttpServletRequest
     * @param messageKey the message key to look up bugzilla 2264
     */
    protected String getMessageForKey(HttpServletRequest request, String messageKey) {
        if (null == messageKey) {
            return null;
        }
        // Return the message for the user's locale.
        return MessageUtil.getMessage(messageKey);
        // return ResourceLocator.getInstance().getMessageResources().getMessage(
        // locale, messageKey);
    }

    // bugzilla 2264
    protected String getMessageForKey(HttpServletRequest request, String messageKey, String arg0) {
        if (null == messageKey) {
            return null;
        }
        // Return the message for the user's locale.
        return MessageUtil.getMessage(messageKey, new String[] { arg0 });
        // return ResourceLocator.getInstance().getMessageResources().getMessage(
        // locale, messageKey, arg0);
    }

    // bugzilla 1856
    protected void moveParentTestsOfCurrentTestsFromPreviouslyReportedTests(List<ResultsReportTest> currentTests,
            List<?> previouslyReportedTests) {
        // create temp list of previously and currently reported tests
        List<ResultsReportTest> tempPreviouslyReportedTests = new ArrayList<>();
        List<ResultsReportTest> tempCurrentTests = new ArrayList<>();
        for (int i = 0; i < previouslyReportedTests.size(); i++) {
            ResultsReportTest test = (ResultsReportTest) previouslyReportedTests.get(i);
            tempPreviouslyReportedTests.add(test);
        }
        for (int i = 0; i < currentTests.size(); i++) {
            ResultsReportTest test = currentTests.get(i);
            tempCurrentTests.add(test);
        }
        for (int i = 0; i < tempCurrentTests.size(); i++) {
            ResultsReportTest test = tempCurrentTests.get(i);
            recursiveMoveParentTests(test, currentTests, tempCurrentTests, previouslyReportedTests,
                    tempPreviouslyReportedTests);
        }
    }

    // bugzilla 1856
    private void recursiveMoveParentTests(ResultsReportTest test, List<ResultsReportTest> currentTests,
            List<ResultsReportTest> tempCurrentTests, List<?> previouslyReportedTests,
            List<ResultsReportTest> tempPreviouslyReportedTests) {
        Iterator<ResultsReportTest> it = tempPreviouslyReportedTests.iterator();
        while (it.hasNext()) {
            ResultsReportTest previousTest = it.next();
            if (test.getAnalysis().getParentAnalysis() != null
                    && test.getAnalysis().getParentAnalysis().getId().equals(previousTest.getAnalysis().getId())) {
                // we are removing this one from previouslyReportedTests - it is a parent of a
                // currently reported test (adding to currentTests)
                currentTests.add(previousTest);
                previouslyReportedTests.remove(previousTest);
                // now the test to find possible further parent of is this parent
                recursiveMoveParentTests(previousTest, currentTests, tempCurrentTests, previouslyReportedTests,
                        tempPreviouslyReportedTests);
                break;
            }
        }
    }

    // bugzilla 1856 add in "phantom" tests to complete the hierarchy so we can sort
    // correctly (the phantom tests are removed after)
    protected List<ResultsReportTest> completeHierarchyOfTestsForSorting(List<?> tests) {
        List<ResultsReportTest> newTests = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++) {
            ResultsReportTest test = (ResultsReportTest) tests.get(i);
            newTests.add(test);
            recursiveHierarchyBuild(test, tests, newTests);
        }
        return newTests;
    }

    // bugzilla 1856
    protected List<ResultsReportTest> removePhantomTests(List<ResultsReportTest> tests) {
        Iterator<ResultsReportTest> it = tests.iterator();
        tests = new ArrayList<>();
        while (it.hasNext()) {
            ResultsReportTest test = it.next();
            if (!test.isPhantom()) {
                tests.add(test);
            }
        }
        return tests;
    }

    // bugzila 1856 recursively add in phantom tests to build the actual original
    // hierarchy
    // this is so that tests that are part of parent/child relationships can be
    // taken into account for sorting
    private void recursiveHierarchyBuild(ResultsReportTest element, List<?> tests, List<ResultsReportTest> newTests) {
        if (element != null && element.getAnalysis().getParentAnalysis() != null) {
            // find out if parent is already in the original list
            boolean alreadyInList = false;
            for (int i = 0; i < tests.size(); i++) {
                ResultsReportTest test = (ResultsReportTest) tests.get(i);
                if (element.getAnalysis().getParentAnalysis().getId().equals(test.getAnalysis().getId())) {
                    alreadyInList = true;
                }
            }
            if (!alreadyInList) {
                // add phantom test to list
                Analysis analysis = new Analysis();
                analysis.setId(element.getAnalysis().getParentAnalysis().getId());
                analysisService.getData(analysis);
                ResultsReportTest test = new ResultsReportTest();
                test.setAnalysis(analysis);
                test.setPhantom(true);
                newTests.add(test);
                // now check for another parent
                recursiveHierarchyBuild(test, tests, newTests);
            }
        }
    }
}
