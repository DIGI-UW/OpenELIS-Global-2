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

import java.io.Serializable;
import java.util.Objects;

public class TestSchemaPK implements Serializable {

    private static final long serialVersionUID = -4708664634912884009L;

    private String testId;
    private String codeTypeId;

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getCodeTypeId() {
        return codeTypeId;
    }

    public void setCodeTypeId(String codeTypeId) {
        this.codeTypeId = codeTypeId;
    }

    public String toString() {
        return testId + codeTypeId;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TestSchemaPK that = (TestSchemaPK) o;

        return Objects.equals(this.testId, that.testId) && Objects.equals(this.codeTypeId, that.codeTypeId);
    }

    public int hashCode() {
        return Objects.hash(testId, codeTypeId);
    }
}
