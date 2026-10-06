package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

public record AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
        AnalyzerUnresolvedReason unresolvedReason) {

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId) {
        this(sourceRowKey, mappingState, testId, null);
    }
}
