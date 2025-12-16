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
package org.openelisglobal.testresult.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.test.valueholder.Test;

public class TestResult extends BaseObject<String> {

    private static final long serialVersionUID = 1L;
    private String id;
    private ValueHolderInterface test;
    private String testName;
    private String resultGroup;
    private String sortOrder;
    private String flags;
    private String testResultType;
    private String value;
    private String significantDigits;
    private String quantLimit;
    private String contLevel;
    private String scriptletName;
    private ValueHolderInterface scriptlet;
    private Boolean isQuantifiable = false;
    private Boolean isNormal = false;
    private Boolean isActive = true;

    // transient
    private Boolean isDefault = false;

    public TestResult() {
        super();
        this.test = new ValueHolder();
        this.scriptlet = new ValueHolder();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public Test getTest() {
        return (Test) this.test.getValue();
    }

    public void setTest(Test test) {
        this.test.setValue(test);
    }

    protected ValueHolderInterface getTestHolder() {
        return this.test;
    }

    public String getTestName() {
        return this.testName;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    protected void setTestHolder(ValueHolderInterface test) {
        this.test = test;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getContLevel() {
        return contLevel;
    }

    public void setContLevel(String contLevel) {
        this.contLevel = contLevel;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public String getQuantLimit() {
        return quantLimit;
    }

    public void setQuantLimit(String quantLimit) {
        this.quantLimit = quantLimit;
    }

    public String getResultGroup() {
        return resultGroup;
    }

    public void setResultGroup(String resultGroup) {
        this.resultGroup = resultGroup;
    }

    public String getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(String significantDigits) {
        this.significantDigits = significantDigits;
    }

    public String getTestResultType() {
        return testResultType;
    }

    public void setTestResultType(String testResultType) {
        this.testResultType = testResultType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // gnr jackson setter conflict
    // public void setTest(ValueHolderInterface test) {
    // this.test = test;
    // }

    public String getScriptletName() {
        return scriptletName;
    }

    public void setScriptletName(String scriptletName) {
        this.scriptletName = scriptletName;
    }

    public void setScriptlet(Scriptlet scriptlet) {
        this.scriptlet.setValue(scriptlet);
    }

    protected void setScriptletHolder(ValueHolderInterface scriptlet) {
        this.scriptlet = scriptlet;
    }

    public Scriptlet getScriptlet() {
        return (Scriptlet) this.scriptlet.getValue();
    }

    protected ValueHolderInterface getScriptletHolder() {
        return this.scriptlet;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsQuantifiable() {
        return isQuantifiable;
    }

    public void setIsQuantifiable(Boolean isQuantifiable) {
        this.isQuantifiable = isQuantifiable;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsNormal() {
        return isNormal;
    }

    public void setIsNormal(Boolean normal) {
        isNormal = normal;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getDefault() {
        return isDefault;
    }
}
