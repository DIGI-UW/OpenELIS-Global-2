package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerSiteBindingConfirmationRequest(String baseBindingFingerprint, String recognitionFingerprint,
        List<AnalyzerSiteBindingSourceRow> confirmedRows, List<AnalyzerSiteBindingSourceRow> excludedRows) {

    public AnalyzerSiteBindingConfirmationRequest {
        confirmedRows = confirmedRows == null ? List.of() : List.copyOf(confirmedRows);
        excludedRows = excludedRows == null ? List.of() : List.copyOf(excludedRows);
    }
}
