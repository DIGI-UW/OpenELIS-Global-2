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

import java.io.Serializable;

// VIEW TEST_ANALYTE_TEST_RESULT is mapped to this valueholder

public class TestAnalyteTestResult implements Serializable {

    private TestAnalyteTestResult id;

    private String testAnalyteTestResultId;

    private String analyteName;

    private String externalId;

    private String testId;

    private String testAnalyteId;

    private String testResultId;

    private String resultGroup;

    private String resultValue;

    private String resultType;

    private String testAnalyteType;

    private String sortOrder;

    private String isActive;

    public TestAnalyteTestResult() {
        super();
    }

    public void setId(TestAnalyteTestResult id) {
        this.id = id;
    }

    public TestAnalyteTestResult getId() {
        return this.id;
    }

    public String getAnalyteName() {
        return this.analyteName;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public String getIsActive() {
        return this.isActive;
    }

    public String getResultGroup() {
        return this.resultGroup;
    }

    public String getResultType() {
        return this.resultType;
    }

    public String getResultValue() {
        return this.resultValue;
    }

    public String getSortOrder() {
        return this.sortOrder;
    }

    public String getTestAnalyteId() {
        return this.testAnalyteId;
    }

    public String getTestAnalyteType() {
        return this.testAnalyteType;
    }

    public String getTestId() {
        return this.testId;
    }

    public String getTestResultId() {
        return this.testResultId;
    }

    public void setAnalyteName(String analyteName) {
        this.analyteName = analyteName;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public void setResultGroup(String resultGroup) {
        this.resultGroup = resultGroup;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setTestAnalyteId(String testAnalyteId) {
        this.testAnalyteId = testAnalyteId;
    }

    public void setTestAnalyteType(String testAnalyteType) {
        this.testAnalyteType = testAnalyteType;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public void setTestResultId(String testResultId) {
        this.testResultId = testResultId;
    }

    public String getTestAnalyteTestResultId() {
        return testAnalyteTestResultId;
    }

    public void setTestAnalyteTestResultId(String testAnalyteTestResultId) {
        this.testAnalyteTestResultId = testAnalyteTestResultId;
    }
}
