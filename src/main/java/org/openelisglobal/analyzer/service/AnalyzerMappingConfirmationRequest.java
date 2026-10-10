package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerMappingConfirmationRequest(String baseBindingFingerprint, String recognitionFingerprint,
        List<AnalyzerMappingSourceRow> confirmedRows, List<AnalyzerMappingSourceRow> excludedRows) {

    public AnalyzerMappingConfirmationRequest {
        confirmedRows = confirmedRows == null ? List.of() : List.copyOf(confirmedRows);
        excludedRows = excludedRows == null ? List.of() : List.copyOf(excludedRows);
    }
}
