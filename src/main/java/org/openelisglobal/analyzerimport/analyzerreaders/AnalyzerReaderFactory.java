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
package org.openelisglobal.analyzerimport.analyzerreaders;

public class AnalyzerReaderFactory {

    /*
     * A little history. We have changed from a factory pattern to a strategy
     * pattern but this is being left as a factory because we are assuming that at
     * some point we are going to be reading more than flat files
     */
    public static AnalyzerReader getReaderFor(String name) {
        if (name.endsWith(".xls")) {
            return new AnalyzerXLSLineReader();
        }
        if (name.equals("astm")) {
            return new ASTMAnalyzerReader();
        }
        return new AnalyzerLineReader();
    }
}
