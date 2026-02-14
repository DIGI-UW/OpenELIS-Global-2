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

import java.util.List;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

public abstract class AnalyzerLineInserter {

    /**
     * Analyzer results service (lazy-initialized to allow unit testing without
     * Spring context).
     */
    private AnalyzerResultsService analyzerResultService;

    /**
     * Get analyzer results service (lazy initialization).
     *
     * <p>
     * This uses lazy initialization instead of eager field initialization to allow
     * unit tests to instantiate LineInserter subclasses without requiring a full
     * Spring context. The service is only retrieved when actually needed for
     * persistence operations.
     *
     * @return AnalyzerResultsService instance
     */
    protected AnalyzerResultsService getAnalyzerResultService() {
        if (analyzerResultService == null) {
            analyzerResultService = SpringContext.getBean(AnalyzerResultsService.class);
        }
        return analyzerResultService;
    }

    protected void persistResults(List<AnalyzerResults> results, String systemUserId) {
        getAnalyzerResultService().insertAnalyzerResults(results, systemUserId);
    }

    protected boolean persistImport(String currentUserId, List<AnalyzerResults> results) {

        if (results.size() > 0) {
            for (AnalyzerResults analyzerResults : results) {
                if ("-1".equals(analyzerResults.getTestId())) {
                    analyzerResults.setTestId(null);
                    analyzerResults.setReadOnly(true);
                }
            }

            try {
                persistResults(results, currentUserId);
            } catch (LIMSRuntimeException e) {
                LogEvent.logDebug(e);
                return false;
            }
        }
        return true;
    }

    public abstract boolean insert(List<String> lines, String currentUserId);

    public abstract String getError();
}
