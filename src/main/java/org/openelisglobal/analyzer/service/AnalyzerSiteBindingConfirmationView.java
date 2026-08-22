package org.openelisglobal.analyzer.service;

import java.time.Instant;
import java.util.List;

public record AnalyzerSiteBindingConfirmationView(State state, String profileId, int profileRevision,
        String bindingFingerprint, String recognitionFingerprint, String confirmedBy, String confirmedByDisplayName,
        Instant confirmedAt, List<AnalyzerSiteBindingSourceRow> confirmedRows,
        List<AnalyzerSiteBindingSourceRow> excludedRows) {

    public enum State {
        UNCONFIRMED, CURRENT, STALE
    }

    public AnalyzerSiteBindingConfirmationView {
        confirmedRows = confirmedRows == null ? List.of() : List.copyOf(confirmedRows);
        excludedRows = excludedRows == null ? List.of() : List.copyOf(excludedRows);
    }

    public static AnalyzerSiteBindingConfirmationView unconfirmed() {
        return new AnalyzerSiteBindingConfirmationView(State.UNCONFIRMED, null, 0, null, null, null, null, null,
                List.of(), List.of());
    }
}
