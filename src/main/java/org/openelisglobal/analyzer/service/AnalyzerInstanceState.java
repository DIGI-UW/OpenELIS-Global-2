package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/** OpenELIS-owned analyzer instance state plus its Bridge references. */
public record AnalyzerInstanceState(String analyzerId, String name, List<String> labUnitIds, String profileId,
        int profileRevision, String profileFingerprint, String bridgeConnectionId, Analyzer.AnalyzerStatus status,
        long heldResultCount) {

    public AnalyzerInstanceState {
        labUnitIds = labUnitIds == null ? List.of() : List.copyOf(labUnitIds);
    }

    public AnalyzerInstanceState withBridgeConnectionId(String connectionId) {
        return new AnalyzerInstanceState(analyzerId, name, labUnitIds, profileId, profileRevision, profileFingerprint,
                connectionId, status, heldResultCount);
    }
}
