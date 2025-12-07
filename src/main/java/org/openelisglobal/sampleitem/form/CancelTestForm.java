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
package org.openelisglobal.sampleitem.form;

import jakarta.validation.constraints.NotBlank;

/**
 * Form object for cancelling/removing a test from a sample item.
 *
 * <p>
 * Contains the analysis ID to cancel and the sample item ID for validation.
 *
 * <p>
 * Related: Feature 001-sample-management
 */
public class CancelTestForm {

    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    @NotBlank(message = "Sample item ID is required")
    private String sampleItemId;

    // ========== Constructors ==========

    public CancelTestForm() {
    }

    public CancelTestForm(String analysisId, String sampleItemId) {
        this.analysisId = analysisId;
        this.sampleItemId = sampleItemId;
    }

    // ========== Getters and Setters ==========

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }
}
