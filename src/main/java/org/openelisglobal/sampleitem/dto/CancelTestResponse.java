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

/**
 * Response object for test cancellation operations.
 *
 * <p>
 * Returns the result of the cancellation, including the analysis ID and a
 * success message.
 *
 * <p>
 * Related: Feature 001-sample-management
 */
public class CancelTestResponse {

    private String analysisId;
    private String testName;
    private boolean success;
    private String message;

    // ========== Constructors ==========

    public CancelTestResponse() {
    }

    public CancelTestResponse(String analysisId, String testName, boolean success, String message) {
        this.analysisId = analysisId;
        this.testName = testName;
        this.success = success;
        this.message = message;
    }

    // ========== Getters and Setters ==========

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
