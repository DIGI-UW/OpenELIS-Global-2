package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;

public record AnalyzerSiteBindingResultDraft(String sourceRowKey, String rawValue,
        AnalyzerSiteBindingMappingState mappingState, String testResultId) {
}
