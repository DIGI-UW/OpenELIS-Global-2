package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;

public record AnalyzerSiteBindingTestDraft(String sourceRowKey, AnalyzerSiteBindingMappingState mappingState,
        String testId) {
}
