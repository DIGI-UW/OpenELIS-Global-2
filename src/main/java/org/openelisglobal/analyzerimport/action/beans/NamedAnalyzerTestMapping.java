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
package org.openelisglobal.analyzerimport.action.beans;

public class NamedAnalyzerTestMapping {

    private String analyzerName;
    private String analyzerTestName;
    private String actualTestName;
    private String uniqueId;
    private static final String uniqueIdSeperator = "#";

    public String getAnalyzerName() {
        return analyzerName;
    }

    public void setAnalyzerName(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public String getAnalyzerTestName() {
        return analyzerTestName;
    }

    public void setAnalyzerTestName(String analyzerTestName) {
        this.analyzerTestName = analyzerTestName;
    }

    public String getActualTestName() {
        return actualTestName;
    }

    public void setActualTestName(String actualTestName) {
        this.actualTestName = actualTestName;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        if (uniqueId == null) {
            uniqueId = analyzerName + uniqueIdSeperator + analyzerTestName + uniqueIdSeperator + actualTestName;
        }
        return uniqueId;
    }

    public static String getUniqueIdSeperator() {
        return uniqueIdSeperator;
    }
}
