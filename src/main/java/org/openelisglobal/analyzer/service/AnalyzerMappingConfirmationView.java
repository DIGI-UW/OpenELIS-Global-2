package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;

public record AnalyzerMappingConfirmationView(State state, String profileId, int profileRevision,
        String mappingFingerprint, String recognitionFingerprint, String confirmedBy, String confirmedByDisplayName,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant confirmedAt, List<AnalyzerMappingSourceRow> confirmedRows,
        List<AnalyzerMappingSourceRow> excludedRows) {

    public enum State {
        UNCONFIRMED, CURRENT, STALE
    }

    public AnalyzerMappingConfirmationView {
        confirmedRows = confirmedRows == null ? List.of() : List.copyOf(confirmedRows);
        excludedRows = excludedRows == null ? List.of() : List.copyOf(excludedRows);
    }

    public static AnalyzerMappingConfirmationView unconfirmed() {
        return new AnalyzerMappingConfirmationView(State.UNCONFIRMED, null, 0, null, null, null, null, null, List.of(),
                List.of());
    }
}
