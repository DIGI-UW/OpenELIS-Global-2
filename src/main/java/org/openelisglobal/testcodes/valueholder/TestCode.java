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
package org.openelisglobal.testcodes.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class TestCode extends BaseObject<TestSchemaPK> {

    private static final long serialVersionUID = -4020747098359946800L;

    private TestSchemaPK compoundId = new TestSchemaPK();
    private String value;

    public TestSchemaPK getCompoundId() {
        return compoundId;
    }

    public void setCompoundId(TestSchemaPK compoundId) {
        this.compoundId = compoundId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTestId() {
        return compoundId.getTestId();
    }

    public void setTestId(String testId) {
        compoundId.setTestId(testId);
    }

    public String getCodeTypeId() {
        return compoundId.getCodeTypeId();
    }

    public void setCodeTypeId(String codeTypeId) {
        compoundId.setCodeTypeId(codeTypeId);
    }

    @Override
    public void setId(TestSchemaPK id) {
        setCompoundId(id);
    }

    @Override
    public TestSchemaPK getId() {
        return getCompoundId();
    }
}
