package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;

/**
 * An analyzer's mapping as the editor shows it, or, with no analyzer, the
 * defaults a new analyzer on the profile revision would get.
 */
public record AnalyzerMappingView(String analyzerId, String profileId, int profileRevision, String profileFingerprint,
        String displayName, String protocol, String mappingId, int mappingRevision, String mappingFingerprint,
        List<TestRow> tests, BridgeProfileCatalog.ControlRecognitionSummary controlRecognition,
        AnalyzerMappingConfirmationView confirmation) {

    public AnalyzerMappingView {
        tests = tests == null ? List.of() : List.copyOf(tests);
        confirmation = confirmation == null ? AnalyzerMappingConfirmationView.unconfirmed() : confirmation;
    }

    /**
     * One record of the mapping. On a main record, {@code enabled} and
     * {@code instrumentCode} say whether this instrument runs the assay and the
     * code it sends (null for the profile's own).
     */
    public record TestRow(String sourceRowKey, String rawCode, List<String> aliases, String testNameHint, String loinc,
            String unit, String resultType, BridgeAnalyzerProfile.NormalizedCoding normalizedCoding,
            AnalyzerMappingState mappingState, AnalyzerMappingOrigin origin, String testId, String componentId,
            AnalyzerMappingCatalogService.TestOption selectedTest,
            AnalyzerMappingCatalogService.TestOption suggestedTest, AnalyzerUnresolvedReason unresolvedReason,
            List<ResultRow> results, String subIdentity, String callComponentId, String componentCode,
            String callComponentCode, boolean enabled, String instrumentCode) {

        public TestRow {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            results = results == null ? List.of() : List.copyOf(results);
        }
    }

    public record ResultRow(String rawValue, AnalyzerMappingState mappingState, AnalyzerMappingOrigin origin,
            String resultOptionId, AnalyzerMappingCatalogService.ResultOption selectedOption,
            AnalyzerMappingCatalogService.ResultOption suggestedOption, AnalyzerUnresolvedReason unresolvedReason,
            boolean observed, String translationOf) {
    }
}
