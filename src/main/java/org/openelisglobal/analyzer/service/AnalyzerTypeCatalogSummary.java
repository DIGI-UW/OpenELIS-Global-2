package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerTypeCatalogSummary(String profileId, int revision, String displayName, String category,
        String protocol, String source, String status, String manufacturer, String model, String legacyVersion,
        String parentProfileId, Integer parentRevision, boolean connectionTestSupported, String bridgeFingerprint,
        BridgeProfileAudit bridgeAudit, AnalyzerTypeMappingProgress testMappings,
        AnalyzerTypeMappingProgress resultValueMappings, int qcIdentificationRuleCount, long analyzerCount,
        AnalyzerTypeSiteBindingSummary siteBinding, List<AnalyzerTypeAttentionCode> attentionCodes) {

    public AnalyzerTypeCatalogSummary {
        attentionCodes = attentionCodes == null ? List.of() : List.copyOf(attentionCodes);
    }
}
