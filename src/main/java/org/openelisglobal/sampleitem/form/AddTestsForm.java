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

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

/**
 * Form object for adding tests to one or more sample items.
 *
 * <p>
 * Supports both single-sample and bulk operations (User Stories 2 and 4).
 * Includes Jakarta validation annotations for request validation at the
 * controller layer per Constitution CR-008.
 *
 * <p>
 * Related: Feature 001-sample-management, User Stories 2 and 4
 *
 * @see org.openelisglobal.sampleitem.dto.AddTestsResponse
 */
public class AddTestsForm {

    @NotEmpty(message = "At least one sample item ID is required")
    private List<String> sampleItemIds = new ArrayList<>();

    @NotEmpty(message = "At least one test ID is required")
    private List<String> testIds = new ArrayList<>();

    // ========== Constructors ==========

    public AddTestsForm() {
    }

    public AddTestsForm(List<String> sampleItemIds, List<String> testIds) {
        this.sampleItemIds = sampleItemIds;
        this.testIds = testIds;
    }

    // ========== Getters and Setters ==========

    public List<String> getSampleItemIds() {
        return sampleItemIds;
    }

    public void setSampleItemIds(List<String> sampleItemIds) {
        this.sampleItemIds = sampleItemIds;
    }

    public List<String> getTestIds() {
        return testIds;
    }

    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }
}
