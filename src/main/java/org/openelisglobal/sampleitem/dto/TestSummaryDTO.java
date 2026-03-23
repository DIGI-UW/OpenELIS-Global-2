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
package org.openelisglobal.sampleitem.dto;

import java.sql.Timestamp;

/**
 * Summary Data Transfer Object for test orders associated with sample items.
 *
 * <p>
 * Lightweight DTO used for displaying ordered tests in sample item details.
 * Contains essential test information without full nested relationships.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see SampleItemDTO#orderedTests
 */
public class TestSummaryDTO {

    private String analysisId;
    private String testId;
    private String testName;
    private String status;
    private Timestamp orderedDate;

    // ========== Constructors ==========

    public TestSummaryDTO() {
    }

    public TestSummaryDTO(String analysisId, String testId, String testName, String status, Timestamp orderedDate) {
        this.analysisId = analysisId;
        this.testId = testId;
        this.testName = testName;
        this.status = status;
        this.orderedDate = orderedDate;
    }

    // ========== Getters and Setters ==========

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

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderedDate() {
        return orderedDate;
    }

    public void setOrderedDate(Timestamp orderedDate) {
        this.orderedDate = orderedDate;
    }
}
