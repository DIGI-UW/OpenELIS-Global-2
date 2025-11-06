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
package org.openelisglobal.testanalyte.valueholder;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.test.valueholder.Test;

public class TestAnalyte extends EnumValueItemImpl {

    private String id;

    private ValueHolderInterface test;

    private ValueHolderInterface analyte;

    // testing one-to-many
    private List testResults;

    private String resultGroup;

    private String testAnalyteType;

    private String sortOrder;

    private String isReportable;

    public TestAnalyte() {
        super();
        this.test = new ValueHolder();
        this.analyte = new ValueHolder();
        this.testResults = new ArrayList();
    }

    public String getId() {
        return this.id;
    }

    public Test getTest() {
        return (Test) this.test.getValue();
    }

    public void setTest(Test test) {
        this.test.setValue(test);
    }

    public Analyte getAnalyte() {
        return (Analyte) this.analyte.getValue();
    }

    public void setAnalyte(Analyte analyte) {
        this.analyte.setValue(analyte);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTestAnalyteType() {
        return testAnalyteType;
    }

    public void setTestAnalyteType(String testAnalyteType) {
        this.testAnalyteType = testAnalyteType;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List getTestResults() {
        return testResults;
    }

    public void setTestResults(List testResults) {
        this.testResults = testResults;
    }

    public String getResultGroup() {
        return resultGroup;
    }

    public void setResultGroup(String resultGroup) {
        this.resultGroup = resultGroup;
    }

    public String getIsReportable() {
        return isReportable;
    }

    public void setIsReportable(String isReportable) {
        this.isReportable = isReportable;
    }
}
