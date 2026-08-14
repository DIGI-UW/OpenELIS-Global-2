package org.openelisglobal.analyzer.service;

import java.util.Comparator;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;

public record AnalyzerSiteBindingTestDraft(String sourceRowKey, String rawAnalyzerCode, List<String> aliases,
        String displayName, String resultType, String normalizedSystem, String normalizedCode,
        AnalyzerSiteBindingTest.MappingState mappingState, String testId, String componentId) {

    public AnalyzerSiteBindingTestDraft {
        sourceRowKey = trimToNull(sourceRowKey);
        rawAnalyzerCode = trimToNull(rawAnalyzerCode);
        aliases = aliases == null ? List.of()
                : aliases.stream().map(AnalyzerSiteBindingTestDraft::trimToNull)
                        .sorted(Comparator.nullsFirst(String::compareTo)).distinct().toList();
        displayName = trimToNull(displayName);
        resultType = trimToNull(resultType);
        normalizedSystem = trimToNull(normalizedSystem);
        normalizedCode = trimToNull(normalizedCode);
        testId = trimToNull(testId);
        componentId = trimToNull(componentId);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
