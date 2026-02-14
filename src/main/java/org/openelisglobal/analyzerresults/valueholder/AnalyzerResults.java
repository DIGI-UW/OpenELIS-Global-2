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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzerresults.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;

public class AnalyzerResults extends BaseObject<String> implements Cloneable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String analyzerId;
    private String accessionNumber;
    private String testName;
    private String result;
    private String units;
    private String duplicateAnalyzerResultId;
    private boolean isControl = false;
    private boolean isReadOnly = false;
    private String testId;
    private String resultType = "N";
    private Timestamp completeDate;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber.replaceAll("\'", "");
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestName() {
        return testName;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return this.result;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getUnits() {
        return units;
    }

    public void setIsControl(boolean isControl) {
        this.isControl = isControl;
    }

    public boolean getIsControl() {
        return isControl;
    }

    public void setCompleteDate(Timestamp completeDate) {
        this.completeDate = completeDate;
    }

    public Timestamp getCompleteDate() {
        return completeDate;
    }

    public String getCompleteDateForDisplay() {
        return DateUtil.convertTimestampToStringDate(completeDate);
    }

    public void setDuplicateAnalyzerResultId(String duplicateAnalyzerResultId) {
        this.duplicateAnalyzerResultId = duplicateAnalyzerResultId;
    }

    public String getDuplicateAnalyzerResultId() {
        return duplicateAnalyzerResultId;
    }

    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestId() {
        return testId;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultType() {
        return resultType;
    }
}
