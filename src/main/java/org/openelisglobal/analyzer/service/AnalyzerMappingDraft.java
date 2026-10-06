package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerMappingDraft(List<AnalyzerMappingTestDraft> tests, List<AnalyzerMappingResultDraft> results) {

    public AnalyzerMappingDraft {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }

    /** The decisions a saved revision holds, as a draft. */
    public static AnalyzerMappingDraft of(AnalyzerMappingSnapshot snapshot) {
        return new AnalyzerMappingDraft(
                snapshot.tests().stream()
                        .map(row -> new AnalyzerMappingTestDraft(row.getId().getSourceRowKey(), row.getMappingState(),
                                row.getTestId(), row.getComponentId(), row.getUnresolvedReason(), row.getOrigin(),
                                row.getId().getSubIdentity(), row.getCallComponentId(), row.isEnabled(),
                                row.getInstrumentCode()))
                        .toList(),
                snapshot.results().stream()
                        .map(row -> new AnalyzerMappingResultDraft(row.getId().getSourceRowKey(),
                                row.getId().getRawValue(), row.getMappingState(), row.getTestResultId(),
                                row.getUnresolvedReason(), row.getOrigin(), row.getId().getSubIdentity()))
                        .toList());
    }
}
