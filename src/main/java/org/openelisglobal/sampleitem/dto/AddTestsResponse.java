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

import java.util.ArrayList;
import java.util.List;

/**
 * Response object for bulk test addition operations.
 *
 * <p>
 * Returns the count of successfully added tests and detailed results for each
 * sample item, including any errors or warnings (e.g., duplicate test skipped).
 *
 * <p>
 * Related: Feature 001-sample-management, User Stories 2 and 4
 *
 * @see org.openelisglobal.sampleitem.form.AddTestsForm
 */
public class AddTestsResponse {

    private int successCount;
    private List<TestAdditionResult> results = new ArrayList<>();

    // ========== Constructors ==========

    public AddTestsResponse() {
    }

    public AddTestsResponse(int successCount, List<TestAdditionResult> results) {
        this.successCount = successCount;
        this.results = results;
    }

    // ========== Getters and Setters ==========

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public List<TestAdditionResult> getResults() {
        return results;
    }

    public void setResults(List<TestAdditionResult> results) {
        this.results = results;
    }

    // ========== Nested Class ==========

    /**
     * Result of adding tests to a single sample item.
     *
     * <p>
     * Includes success status, sample item ID, and any error/warning messages.
     */
    public static class TestAdditionResult {
        private String sampleItemId;
        private String sampleItemExternalId;
        private boolean success;
        private String message;
        private List<String> addedTestIds = new ArrayList<>();
        private List<String> skippedTestIds = new ArrayList<>();

        public TestAdditionResult() {
        }

        public TestAdditionResult(String sampleItemId, String sampleItemExternalId, boolean success, String message) {
            this.sampleItemId = sampleItemId;
            this.sampleItemExternalId = sampleItemExternalId;
            this.success = success;
            this.message = message;
        }

        // Getters and Setters

        public String getSampleItemId() {
            return sampleItemId;
        }

        public void setSampleItemId(String sampleItemId) {
            this.sampleItemId = sampleItemId;
        }

        public String getSampleItemExternalId() {
            return sampleItemExternalId;
        }

        public void setSampleItemExternalId(String sampleItemExternalId) {
            this.sampleItemExternalId = sampleItemExternalId;
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

        public List<String> getAddedTestIds() {
            return addedTestIds;
        }

        public void setAddedTestIds(List<String> addedTestIds) {
            this.addedTestIds = addedTestIds;
        }

        public List<String> getSkippedTestIds() {
            return skippedTestIds;
        }

        public void setSkippedTestIds(List<String> skippedTestIds) {
            this.skippedTestIds = skippedTestIds;
        }
    }
}
