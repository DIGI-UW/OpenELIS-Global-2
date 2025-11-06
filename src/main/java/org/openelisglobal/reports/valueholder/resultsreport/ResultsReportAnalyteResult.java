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

import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.result.valueholder.Result;

/**
 * @author benzd1 bugzilla 2264
 */
public class ResultsReportAnalyteResult {

    private Analyte analyte;

    private Result result;

    private String componentName;

    private String resultValue;

    private String testResultSortOrder;

    public String getTestResultSortOrder() {
        return testResultSortOrder;
    }

    public void setTestResultSortOrder(String testResultSortOrder) {
        this.testResultSortOrder = testResultSortOrder;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Analyte getAnalyte() {
        return analyte;
    }

    public void setAnalyte(Analyte analyte) {
        this.analyte = analyte;
    }
}
