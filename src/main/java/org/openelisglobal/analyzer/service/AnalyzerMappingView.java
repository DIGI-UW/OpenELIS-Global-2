package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

public record AnalyzerMappingView(String profileId, int profileRevision, String profileFingerprint, String displayName,
        String protocol, String siteBindingId, int siteBindingRevision, String bindingFingerprint, List<TestRow> tests,
        BridgeProfileCatalog.ControlRecognitionSummary controlRecognition,
        AnalyzerMappingConfirmationView confirmation) {

    public AnalyzerMappingView(String profileId, int profileRevision, String profileFingerprint, String displayName,
            String protocol, String siteBindingId, int siteBindingRevision, String bindingFingerprint,
            List<TestRow> tests, BridgeProfileCatalog.ControlRecognitionSummary controlRecognition) {
        this(profileId, profileRevision, profileFingerprint, displayName, protocol, siteBindingId, siteBindingRevision,
                bindingFingerprint, tests, controlRecognition, AnalyzerMappingConfirmationView.unconfirmed());
    }

    public AnalyzerMappingView {
        tests = tests == null ? List.of() : List.copyOf(tests);
        confirmation = confirmation == null ? AnalyzerMappingConfirmationView.unconfirmed() : confirmation;
    }

    public record TestRow(String sourceRowKey, String rawCode, List<String> aliases, String testNameHint, String loinc,
            String unit, String resultType, BridgeAnalyzerProfile.NormalizedCoding normalizedCoding,
            AnalyzerMappingState mappingState, String testId, AnalyzerMappingCatalogService.TestOption selectedTest,
            AnalyzerMappingCatalogService.TestOption suggestedTest, AnalyzerUnresolvedReason unresolvedReason,
            List<ResultRow> results) {

        public TestRow {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            results = results == null ? List.of() : List.copyOf(results);
        }
    }

    public record ResultRow(String rawValue, AnalyzerMappingState mappingState, String resultOptionId,
            AnalyzerMappingCatalogService.ResultOption selectedOption,
            AnalyzerMappingCatalogService.ResultOption suggestedOption, AnalyzerUnresolvedReason unresolvedReason,
            boolean observed) {
    }
}
