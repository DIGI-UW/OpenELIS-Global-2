package org.openelisglobal.analyzer.service;

import java.util.Set;

public record BridgeRegistrationResult(boolean complete, Set<String> acknowledgedAnalyzerIds, String failure) {

    public BridgeRegistrationResult {
        acknowledgedAnalyzerIds = acknowledgedAnalyzerIds == null ? Set.of() : Set.copyOf(acknowledgedAnalyzerIds);
    }

    public boolean acknowledged(String analyzerId) {
        return complete && acknowledgedAnalyzerIds.contains(analyzerId);
    }
}
