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
package org.openelisglobal.qaevent.valueholder;

import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.valueholder.Test;

public class QaEvent extends BaseObject<String> {

    private String id;

    private String qaEventName;

    private String description;

    private ValueHolderInterface type;
    // Bugzilla 2246
    private String selectedTypeId;

    private String isBillable;

    private String isHoldable;

    private String reportingSequence;

    private String reportingText;

    private ValueHolderInterface test;

    private String selectedTestId;

    // (concatenate qaEvent name/desc)
    private String qaEventDisplayValue;

    // bugzilla 2506
    private ValueHolderInterface category;
    private String selectedCategoryId;

    public QaEvent() {
        super();
        this.test = new ValueHolder();
        this.type = new ValueHolder();
        this.category = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsBillable() {
        return isBillable;
    }

    public void setIsBillable(String isBillable) {
        this.isBillable = isBillable;
    }

    public String getQaEventName() {
        return qaEventName;
    }

    public void setQaEventName(String qaEventName) {
        this.qaEventName = qaEventName;
    }

    public String getReportingSequence() {
        return reportingSequence;
    }

    public void setReportingSequence(String reportingSequence) {
        this.reportingSequence = reportingSequence;
    }

    public String getReportingText() {
        return reportingText;
    }

    public void setReportingText(String reportingText) {
        this.reportingText = reportingText;
    }

    protected void setTestHolder(ValueHolderInterface test) {
        this.test = test;
    }

    protected ValueHolderInterface getTestHolder() {
        return this.test;
    }

    public void setTest(Test test) {
        this.test.setValue(test);
    }

    public Test getTest() {
        return (Test) this.test.getValue();
    }

    protected void setTypeHolder(ValueHolderInterface type) {
        this.type = type;
    }

    protected ValueHolderInterface getTypeHolder() {
        return this.type;
    }

    public void setType(Dictionary type) {
        this.type.setValue(type);
    }

    public Dictionary getType() {
        return (Dictionary) this.type.getValue();
    }

    public String getSelectedTestId() {
        return selectedTestId;
    }

    public void setSelectedTestId(String selectedTestId) {
        this.selectedTestId = selectedTestId;
    }

    // Bugzilla 2246
    public void setSelectedTypeId(String selectedTypeId) {
        this.selectedTypeId = selectedTypeId;
    }

    public String getSelectedTypeId() {
        return selectedTypeId;
    }

    public String getIsHoldable() {
        return isHoldable;
    }

    public void setIsHoldable(String isHoldable) {
        this.isHoldable = isHoldable;
    }

    public String getQaEventDisplayValue() {
        if (!StringUtil.isNullorNill(this.qaEventName)) {
            qaEventDisplayValue = qaEventName + "-" + description;
        } else {
            qaEventDisplayValue = description;
        }
        return qaEventDisplayValue;
    }

    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    protected void setCategoryHolder(ValueHolderInterface category) {
        this.category = category;
    }

    protected ValueHolderInterface getCategoryHolder() {
        return this.category;
    }

    public void setCategory(Dictionary category) {
        this.category.setValue(category);
    }

    public Dictionary getCategory() {
        return (Dictionary) this.category.getValue();
    }

    protected String getDefaultLocalizedName() {
        return getQaEventName();
    }
}
