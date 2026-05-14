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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AnalyzerReader {

    /**
     * Preferred field names for column mapping (sampleId, testCode, result, etc.).
     */
    public static final List<String> PREFERRED_FIELD_ORDER = List.of("sampleId", "testCode", "result", "interpretation",
            "position", "testDate", "testTime");

    public abstract boolean readStream(InputStream stream);

    public abstract boolean insertAnalyzerData(String systemUserId);

    public abstract String getError();

    /**
     * Return parsed records for preview (OGC-324). Default empty; CSV/Excel readers
     * override to return their parsed rows.
     */
    public List<Map<String, String>> getParsedRecords() {
        return Collections.emptyList();
    }

    /**
     * Return tab-separated lines used for inserter.insert (OGC-324 submit). Default
     * empty; file readers override.
     */
    public List<String> getLines() {
        return Collections.emptyList();
    }
}
