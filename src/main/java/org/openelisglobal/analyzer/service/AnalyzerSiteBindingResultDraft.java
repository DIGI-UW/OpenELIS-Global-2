package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;

public record AnalyzerSiteBindingResultDraft(String sourceRowKey, String rawValue,
        AnalyzerSiteBindingMappingState mappingState, String testResultId, AnalyzerUnresolvedReason unresolvedReason) {

    public AnalyzerSiteBindingResultDraft(String sourceRowKey, String rawValue,
            AnalyzerSiteBindingMappingState mappingState, String testResultId) {
        this(sourceRowKey, rawValue, mappingState, testResultId, null);
    }
}
