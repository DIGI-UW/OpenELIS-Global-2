package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * One declared answer's decision: a local answer, or excluded, or unresolved
 * with its reason.
 */
public record AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
        String testResultId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin) {

    public AnalyzerMappingResultDraft {
        origin = origin == null ? AnalyzerMappingOrigin.DEFAULT : origin;
    }

    public AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
            String testResultId, AnalyzerUnresolvedReason unresolvedReason) {
        this(sourceRowKey, rawValue, mappingState, testResultId, unresolvedReason, AnalyzerMappingOrigin.DEFAULT);
    }

    public AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
            String testResultId) {
        this(sourceRowKey, rawValue, mappingState, testResultId, null, AnalyzerMappingOrigin.DEFAULT);
    }
}
