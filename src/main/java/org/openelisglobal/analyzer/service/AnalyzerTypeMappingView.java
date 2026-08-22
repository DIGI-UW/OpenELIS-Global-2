package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;

public record AnalyzerTypeMappingView(String profileId, int profileRevision, String profileFingerprint,
        String displayName, String protocol, String siteBindingId, int siteBindingRevision, String bindingFingerprint,
        List<TestRow> tests, BridgeProfileCatalog.ControlRecognitionSummary controlRecognition,
        AnalyzerSiteBindingConfirmationView confirmation) {

    public AnalyzerTypeMappingView(String profileId, int profileRevision, String profileFingerprint, String displayName,
            String protocol, String siteBindingId, int siteBindingRevision, String bindingFingerprint,
            List<TestRow> tests, BridgeProfileCatalog.ControlRecognitionSummary controlRecognition) {
        this(profileId, profileRevision, profileFingerprint, displayName, protocol, siteBindingId, siteBindingRevision,
                bindingFingerprint, tests, controlRecognition, AnalyzerSiteBindingConfirmationView.unconfirmed());
    }

    public AnalyzerTypeMappingView {
        tests = tests == null ? List.of() : List.copyOf(tests);
        confirmation = confirmation == null ? AnalyzerSiteBindingConfirmationView.unconfirmed() : confirmation;
    }

    public record TestRow(String sourceRowKey, String rawCode, List<String> aliases, String testNameHint, String loinc,
            String unit, String resultType, BridgeAnalyzerProfile.NormalizedCoding normalizedCoding,
            AnalyzerSiteBindingMappingState mappingState, String testId,
            AnalyzerMappingCatalogService.TestOption selectedTest,
            AnalyzerMappingCatalogService.TestOption suggestedTest, List<ResultRow> results) {

        public TestRow {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            results = results == null ? List.of() : List.copyOf(results);
        }
    }

    public record ResultRow(String rawValue, AnalyzerSiteBindingMappingState mappingState, String resultOptionId,
            AnalyzerMappingCatalogService.ResultOption selectedOption) {
    }
}
