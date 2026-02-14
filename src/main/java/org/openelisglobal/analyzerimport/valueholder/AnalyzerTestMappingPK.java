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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.valueholder;

import java.io.Serializable;
import java.util.Objects;

public class AnalyzerTestMappingPK implements Serializable {

    private static final long serialVersionUID = 1L;
    private String analyzerId;
    private String analyzerTestName;

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerTestName() {
        return analyzerTestName;
    }

    public void setAnalyzerTestName(String analyzerTestName) {
        this.analyzerTestName = analyzerTestName;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AnalyzerTestMappingPK that = (AnalyzerTestMappingPK) o;

        return Objects.equals(this.analyzerId, that.analyzerId)
                && Objects.equals(this.analyzerTestName, that.analyzerTestName);
    }

    public int hashCode() {
        return Objects.hash(analyzerId, analyzerTestName);
    }
}
