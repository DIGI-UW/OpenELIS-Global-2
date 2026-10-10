package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

public record AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
        String testResultId, AnalyzerUnresolvedReason unresolvedReason) {

    public AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
            String testResultId) {
        this(sourceRowKey, rawValue, mappingState, testResultId, null);
    }
}
