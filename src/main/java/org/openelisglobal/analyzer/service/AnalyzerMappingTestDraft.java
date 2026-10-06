package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * One profile test's decision: a local test, optionally one component of it, or
 * excluded or unresolved with the reason it could not be resolved.
 */
public record AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
        String componentId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin) {

    public AnalyzerMappingTestDraft {
        origin = origin == null ? AnalyzerMappingOrigin.DEFAULT : origin;
    }

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
            AnalyzerUnresolvedReason unresolvedReason) {
        this(sourceRowKey, mappingState, testId, null, unresolvedReason, AnalyzerMappingOrigin.DEFAULT);
    }

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId) {
        this(sourceRowKey, mappingState, testId, null, null, AnalyzerMappingOrigin.DEFAULT);
    }
}
