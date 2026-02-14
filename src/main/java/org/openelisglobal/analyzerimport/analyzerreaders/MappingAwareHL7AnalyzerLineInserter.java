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
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.service.MappingApplicationResult;
import org.openelisglobal.analyzer.service.MappingApplicationService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Wrapper for HL7 AnalyzerLineInserter that applies field mappings before
 * delegating.
 *
 * <p>
 * test code mapping. Unmapped fields create error records in dashboard.
 */
public class MappingAwareHL7AnalyzerLineInserter extends AnalyzerLineInserter {

    private final AnalyzerLineInserter originalInserter;
    private final Analyzer analyzer;
    private final MappingApplicationService mappingApplicationService;
    private final AnalyzerErrorService analyzerErrorService;
    private String error;

    public MappingAwareHL7AnalyzerLineInserter(AnalyzerLineInserter originalInserter, Analyzer analyzer) {
        this.originalInserter = originalInserter;
        this.analyzer = analyzer;
        this.mappingApplicationService = SpringContext.getBean(MappingApplicationService.class);
        this.analyzerErrorService = SpringContext.getBean(AnalyzerErrorService.class);
        this.error = null;
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        error = null;
        if (lines == null || lines.isEmpty()) {
            error = "Empty HL7 message lines";
            return false;
        }
        if (analyzer == null || analyzer.getId() == null) {
            error = "Analyzer not configured";
            return false;
        }
        try {
            if (!mappingApplicationService.hasActiveMappings(analyzer.getId())) {
                return originalInserter.insert(lines, currentUserId);
            }
            MappingApplicationResult result = mappingApplicationService.applyMappings(analyzer.getId(), lines);
            if (!result.isSuccess()) {
                String errMsg = "Failed to apply HL7 mappings: " + String.join(", ", result.getErrors());
                createError(errMsg, lines);
                error = errMsg;
                return false;
            }
            if (!result.getUnmappedFields().isEmpty()) {
                String errMsg = "Unmapped HL7 fields: " + String.join(", ", result.getUnmappedFields());
                createError(errMsg, lines);
            }
            boolean success = originalInserter.insert(result.getTransformedLines(), currentUserId);
            if (!success) {
                error = originalInserter.getError();
                if (error == null || error.isEmpty()) {
                    error = "HL7 insert failed";
                }
            }
            return success;
        } catch (Exception e) {
            LogEvent.logError("Error in MappingAwareHL7AnalyzerLineInserter: " + e.getMessage(), e);
            String errMsg = "Error applying HL7 mappings: " + e.getMessage();
            createError(errMsg, lines);
            error = errMsg;
            return false;
        }
    }

    @Override
    public String getError() {
        return error;
    }

    private void createError(String errorMessage, List<String> lines) {
        try {
            String raw = String.join("\n", lines);
            analyzerErrorService.createError(analyzer, AnalyzerError.ErrorType.MAPPING, AnalyzerError.Severity.ERROR,
                    errorMessage, raw);
        } catch (Exception e) {
            LogEvent.logError("Failed to create AnalyzerError for HL7: " + e.getMessage(), e);
        }
    }
}
