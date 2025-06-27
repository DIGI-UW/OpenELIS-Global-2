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

package org.openelisglobal.common.services.serviceBeans;

public class ResultSaveBean {

    private boolean hasQualifiedResult;
    private String resultType;
    private String multiSelectResultValues;
    private String testId;
    private String qualifiedResultId;
    private String qualifiedResultValue;
    private String qualifiedDictionaryId;
    private String resultId;
    private String resultValue;
    private String reportable;
    private Double lowerNormalRange;
    private Double upperNormalRange;
    private int significantDigits = 0;

    public boolean isHasQualifiedResult() {
        return hasQualifiedResult;
    }

    public void setHasQualifiedResult(boolean hasQualifiedResult) {
        this.hasQualifiedResult = hasQualifiedResult;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getMultiSelectResultValues() {
        return multiSelectResultValues;
    }

    public void setMultiSelectResultValues(String multiSelectResultValues) {
        this.multiSelectResultValues = multiSelectResultValues;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getQualifiedResultId() {
        return qualifiedResultId;
    }

    public void setQualifiedResultId(String qualifiedResultId) {
        this.qualifiedResultId = qualifiedResultId;
    }

    public String getQualifiedResultValue() {
        return qualifiedResultValue;
    }

    public void setQualifiedResultValue(String qualifiedResultValue) {
        this.qualifiedResultValue = qualifiedResultValue;
    }

    public String getQualifiedDictionaryId() {
        return qualifiedDictionaryId;
    }

    public void setQualifiedDictionaryId(String qualifiedDictionaryId) {
        this.qualifiedDictionaryId = qualifiedDictionaryId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getReportable() {
        return reportable;
    }

    public void setReportable(String reportable) {
        this.reportable = reportable;
    }

    public Double getLowerNormalRange() {
        return lowerNormalRange;
    }

    public void setLowerNormalRange(Double lowerNormalRange) {
        this.lowerNormalRange = lowerNormalRange;
    }

    public Double getUpperNormalRange() {
        return upperNormalRange;
    }

    public void setUpperNormalRange(Double upperNormalRange) {
        this.upperNormalRange = upperNormalRange;
    }

    public int getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(int significantDigits) {
        this.significantDigits = significantDigits;
    }
}
