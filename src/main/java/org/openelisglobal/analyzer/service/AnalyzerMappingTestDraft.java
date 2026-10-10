package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * One profile record's decision: a local test, optionally one component of it,
 * or excluded or unresolved with the reason it could not be resolved. A record
 * is its code and its sub-identity (empty for the main result); a record
 * carrying both a number and a call sends the call to {@code callComponentId}.
 * The main record also says whether this instrument runs the assay
 * ({@code enabled}, null when not stated, which counts as on) and the code it
 * sends for it ({@code instrumentCode}, null for the profile's own code), as on
 * the instrument's host test code table.
 */
public record AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
        String componentId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin, String subIdentity,
        String callComponentId, Boolean enabled, String instrumentCode) {

    public AnalyzerMappingTestDraft {
        origin = origin == null ? AnalyzerMappingOrigin.DEFAULT : origin;
        subIdentity = subIdentity == null ? "" : subIdentity;
        instrumentCode = instrumentCode == null || instrumentCode.isBlank()
                || instrumentCode.trim().equals(sourceRowKey) ? null : instrumentCode.trim();
    }

    public AnalyzerMappingTestDraft(String sourceRowKey, AnalyzerMappingState mappingState, String testId,
            String componentId, AnalyzerUnresolvedReason unresolvedReason, AnalyzerMappingOrigin origin,
            String subIdentity, String callComponentId) {
        this(sourceRowKey, mappingState, testId, componentId, unresolvedReason, origin, subIdentity, callComponentId,
                null, null);
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

    /** Whether the instrument runs this assay; not stated counts as on. */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public AnalyzerMappingTestDraft withAssay(Boolean enabled, String instrumentCode) {
        return new AnalyzerMappingTestDraft(sourceRowKey, mappingState, testId, componentId, unresolvedReason, origin,
                subIdentity, callComponentId, enabled, instrumentCode);
    }
}
