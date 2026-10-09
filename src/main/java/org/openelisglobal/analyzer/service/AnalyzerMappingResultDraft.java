package org.openelisglobal.analyzer.service;

import java.util.Objects;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * One declared answer's decision: a local answer, or excluded, or unresolved
 * with its reason.
 */
public record AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
        String testResultId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin,
        String subIdentity) {

    public AnalyzerMappingResultDraft {
        origin = origin == null ? AnalyzerMappingOrigin.DEFAULT : origin;
        subIdentity = subIdentity == null ? "" : subIdentity;
    }

    public AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
            String testResultId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin) {
        this(sourceRowKey, rawValue, mappingState, testResultId, unresolvedReason, origin, "");
    }

    public AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
            String testResultId, AnalyzerUnresolvedReason unresolvedReason) {
        this(sourceRowKey, rawValue, mappingState, testResultId, unresolvedReason, AnalyzerMappingOrigin.DEFAULT);
    }

    public AnalyzerMappingResultDraft(String sourceRowKey, String rawValue, AnalyzerMappingState mappingState,
            String testResultId) {
        this(sourceRowKey, rawValue, mappingState, testResultId, null, AnalyzerMappingOrigin.DEFAULT);
    }

    /** Whether two decisions give the answer the same state and result option. */
    public boolean sameAnswer(AnalyzerMappingResultDraft other) {
        return other != null && mappingState == other.mappingState && Objects.equals(testResultId, other.testResultId);
    }

    public AnalyzerMappingRowKey rowKey() {
        return new AnalyzerMappingRowKey(sourceRowKey, subIdentity);
    }
}
