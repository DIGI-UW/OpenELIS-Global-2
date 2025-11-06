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
package org.openelisglobal.reports.valueholder.resultsreport;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.reports.valueholder.common.JRHibernateDataSource;

/**
 * @author benzd1 bugzilla 2264
 */
public class ResultsReportTest {

    private Analysis analysis;

    private String testName;

    private String testMessage;

    private String testId;

    private String testDescription;

    private String analysisId;

    // bugzilla 2292
    private String analysisStatus;

    // bugzilla 2292
    private String printedDate;

    private JRHibernateDataSource resultsReportAnalyteResults;

    private List analyteResults;

    // bugzilla 1856
    private List<ResultsReportTest> children;

    // bugzilla 1856 this is used for sorting:
    // parent tests are loaded into the list of tests to sort in order to
    // maintain the original sorting order
    // phantom tests are removed before displaying/reporting
    private boolean isPhantom;

    public List getAnalyteResults() {
        return analyteResults;
    }

    public void setAnalyteResults(List analyteResults) {
        this.analyteResults = analyteResults;
    }

    public JRHibernateDataSource getResultsReportAnalyteResults() {
        return resultsReportAnalyteResults;
    }

    public void setResultsReportAnalyteResults(JRHibernateDataSource resultsReportAnalyteResults) {
        this.resultsReportAnalyteResults = resultsReportAnalyteResults;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestDescription() {
        return testDescription;
    }

    public void setTestDescription(String testDescription) {
        this.testDescription = testDescription;
    }

    public String getTestMessage() {
        return testMessage;
    }

    public void setTestMessage(String testMessage) {
        this.testMessage = testMessage;
    }

    public List<ResultsReportTest> getChildren() {
        return children;
    }

    public void setChildren(List<ResultsReportTest> children) {
        this.children = children;
    }

    public String getPrintedDate() {
        return printedDate;
    }

    public void setPrintedDate(String printedDate) {
        this.printedDate = printedDate;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public boolean isPhantom() {
        return isPhantom;
    }

    public void setPhantom(boolean isPhantom) {
        this.isPhantom = isPhantom;
    }
}
