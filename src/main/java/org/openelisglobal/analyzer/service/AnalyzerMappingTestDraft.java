package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * One profile record's decision: a local test, optionally one component of it,
 * or excluded or unresolved with the reason it could not be resolved. A record
 * is its code and its sub-identity (empty for the main result); a record
 * carrying both a number and a call sends the call to {@code callComponentId}.
 */
public record AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
        String componentId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin, String subIdentity,
        String callComponentId) {

    public AnalyzerMappingTestDraft {
        origin = origin == null ? AnalyzerMappingOrigin.DEFAULT : origin;
        subIdentity = subIdentity == null ? "" : subIdentity;
    }

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
            String componentId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin) {
        this(sourceRowKey, mappingState, testId, componentId, unresolvedReason, origin, "", null);
    }

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
            AnalyzerUnresolvedReason unresolvedReason) {
        this(sourceRowKey, mappingState, testId, null, unresolvedReason, AnalyzerMappingOrigin.DEFAULT);
    }

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId) {
        this(sourceRowKey, mappingState, testId, null, null, AnalyzerMappingOrigin.DEFAULT);
    }

    public AnalyzerMappingRowKey rowKey() {
        return new AnalyzerMappingRowKey(sourceRowKey, subIdentity);
    }
}
